/** Derive a recognisable, masked account number from the id (e.g. "•• 0001"). */
export function maskedId(id: string): string {
  const clean = id.replace(/[^a-z0-9]/gi, '')
  const tail = clean.slice(-4).toUpperCase()
  return tail ? `•• ${tail}` : id
}

/** Format a numeric amount as US currency, e.g. 1234.5 -> "$1,234.50". */
export function formatMoney(value: number | string): string {
  const n = typeof value === 'string' ? parseFloat(value) : value
  if (!Number.isFinite(n)) return '$0.00'
  return '$' + n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}