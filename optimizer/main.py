"""
main.py
FastAPI entrypoint exposing the MILP + RL delay recovery optimiser over HTTP.

The Spring Boot application (see app.optimizer.url in application.properties)
calls POST /optimize with the current disruption state and receives back a
ranked list of feasible recovery options.
"""

from __future__ import annotations

import logging
import time

from fastapi import FastAPI

from models import FeedbackRequest, FeedbackResponse, OptimisationRequest, OptimisationResponse
from optimizer import MILPOptimiser
from rl_agent import ACTIONS, QAgent

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="RwandAir DSS Optimiser",
    description="Hybrid MILP-RL cost optimisation engine for aircraft delay recovery.",
    version="1.0.0",
)

milp_optimiser = MILPOptimiser()
rl_agent = QAgent()


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.post("/optimize", response_model=OptimisationResponse)
def optimize(req: OptimisationRequest) -> OptimisationResponse:
    start = time.perf_counter()

    candidates = milp_optimiser.generate_options(req)
    rl_recommendation = rl_agent.get_recommendation(req)
    candidates.append(rl_recommendation)

    candidates.sort(key=lambda c: (not c.feasible, c.total_cost))
    for i, option in enumerate(candidates, start=1):
        option.rank = i

    recommended_index = 0  # rank 1 is always first after sort
    elapsed_ms = (time.perf_counter() - start) * 1000.0

    logger.info(
        "Optimised delay_event_id=%s -> %d options in %.2f ms",
        req.delay_event_id,
        len(candidates),
        elapsed_ms,
    )

    return OptimisationResponse(
        delay_event_id=req.delay_event_id,
        options=candidates,
        computation_time_ms=round(elapsed_ms, 2),
        recommended_option_index=recommended_index,
    )


@app.post("/feedback", response_model=FeedbackResponse)
def feedback(req: FeedbackRequest) -> FeedbackResponse:
    """
    Reward = (estimated_cost - actual_cost) / 1000: positive when the recovery
    actually came in cheaper than the model predicted (reinforcing that action for
    this state), negative when it ran over. Scaled down to thousands of dollars so a
    single outcome nudges the policy without swamping the domain-expert-seeded Q
    values, which sit in the 0-10 range. next_state is set equal to the current
    state - each delay event is a self-contained episode in this design (there is no
    natural "next state" to observe), so this is a single-step policy update rather
    than a full multi-step MDP rollout.
    """
    if req.chosen_action not in ACTIONS:
        return FeedbackResponse(
            updated=False,
            reward=0.0,
            message=f"'{req.chosen_action}' is not one of the RL agent's actions {ACTIONS} "
            f"(likely a manual override) - no policy update applied.",
        )

    state = rl_agent.get_state(req.optimisation_request)
    action_idx = ACTIONS.index(req.chosen_action)
    reward = (req.estimated_cost - req.actual_cost) / 1000.0

    old_q_value = float(rl_agent.q_table[state][action_idx])
    rl_agent.update(state, action_idx, reward, state)
    new_q_value = float(rl_agent.q_table[state][action_idx])

    logger.info(
        "Feedback applied: state=%s action=%s reward=%.2f Q: %.3f -> %.3f",
        state, req.chosen_action, reward, old_q_value, new_q_value,
    )

    return FeedbackResponse(
        updated=True,
        reward=round(reward, 2),
        old_q_value=round(old_q_value, 4),
        new_q_value=round(new_q_value, 4),
        message="RL policy updated from real-world outcome.",
    )
