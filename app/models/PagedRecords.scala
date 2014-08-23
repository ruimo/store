package models

case class PagedRecords[+T] (
  currentPage: Int, // zero is the first page.
  pageSize: Int,    // the number of items in one page.
  pageCount: Long,  // the number of pages.
  orderBy: OrderBy,
  records: Seq[T]
) {
  lazy val nextPageExists = currentPage + 1 < pageCount
  lazy val prevPageExists = currentPage > 0

  def map[B](f: (T) => B): PagedRecords[B] = PagedRecords[B](
    currentPage,
    pageSize,
    pageCount,
    orderBy,
    records.map(f)
  )
}

