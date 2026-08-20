/** Derive a recognisable, masked account number from the id (e.g. "•• 0001"). */
export function maskedId(id: string): string {
  const clean = id.replace(/[^a-z0-9]/gi, '')
  const tail = clean.slice(-4).toUpperCase()
  return tail ? `•• ${tail}` : id
}
