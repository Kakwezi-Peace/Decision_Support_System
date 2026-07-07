/** Newest-first: higher id means created more recently (ids are sequential). */
export function sortByIdDesc<T extends { id: number }>(items: T[]): T[] {
  return [...items].sort((a, b) => b.id - a.id);
}
