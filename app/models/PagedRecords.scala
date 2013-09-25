package models

case class PagedRecords[T] (
  currentPage: Int,
  pageSize: Int,
  pageCount: Long,
  records: Seq[T]
) {
  lazy val nextPageExists = pageSize < pageCount
  lazy val prevPageExists = currentPage > 0
}

