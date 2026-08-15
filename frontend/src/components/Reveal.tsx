import type { ReactNode } from 'react'

export function Reveal({
  children,
  delay = 0,
  className = ''
}: {
  children: ReactNode
  delay?: number
  className?: string
}) {
  return (
    <div className={`reveal ${className}`} style={{ animationDelay: `${delay}ms` }}>
      {children}
    </div>
  )
}