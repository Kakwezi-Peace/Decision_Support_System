interface PaginationProps {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null;

  return (
    <div className="pagination">
      <button className="secondary" disabled={page === 1} onClick={() => onPageChange(page - 1)}>
        ← Prev
      </button>
      <span className="pagination-status">
        Page {page} of {totalPages}
      </span>
      <button className="secondary" disabled={page === totalPages} onClick={() => onPageChange(page + 1)}>
        Next →
      </button>
    </div>
  );
}
