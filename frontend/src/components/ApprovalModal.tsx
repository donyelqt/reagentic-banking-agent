import type { Step } from '../types'

export default function ApprovalModal({ steps, onApprove, onCancel, busy }: { steps: Step[]; onApprove: () => void; onCancel: () => void; busy?: boolean }) {
  
  // Helper function to intercept the raw data and format it nicely
  const formatStepDetails = (args: any) => {
    try {
      const parsedArgs = typeof args === 'string' ? JSON.parse(args) : args;

      if (parsedArgs && parsedArgs.amount) {
        const from = parsedArgs.from ? `${parsedArgs.from} → ` : '';
        const dest = parsedArgs.to ? `to ${parsedArgs.to}` : '';
        const destPart = dest ? ` ${dest}` : '';
        return `${from}Amount: $${parsedArgs.amount}${destPart}`;
      }

      return 'Action details attached';
    } catch {
      return 'Action details attached';
    }
  };

  return (
    <div className="absolute inset-0 z-20 grid place-items-end sm:place-items-center p-3 backdrop-in bg-[#0A0B14]/80">
      <div className="glass rounded-[24px] p-5 w-full max-w-md modal-in shadow-lift">
        <div className="flex items-center gap-2 mb-3">
          <span className="w-7 h-7 rounded-full grid place-items-center bg-gold/15 text-gold font-display">!</span>
          <p className="font-display text-lg">Confirmation required</p>
        </div>
        <p className="text-sm text-muted mb-3">The agent prepared these steps. Approve to execute.</p>
        <ul className="space-y-2 mb-4">
          {steps.map((s) => (
            <li key={s.stepId} className="flex items-center justify-between rounded-xl border border-line bg-bg px-3 py-2 text-sm">
              <span className="font-medium capitalize">{s.tool.replace(/([A-Z])/g, ' $1')}</span>
              {/* Replaced JSON.stringify with our new helper function */}
              <span className="text-sm font-medium truncate max-w-[52%] text-right">
                {formatStepDetails(s.args)}
              </span>
            </li>
          ))}
        </ul>
        <div className="flex gap-2">
          <button onClick={onApprove} disabled={busy} className="btn btn-accent flex-1">{busy ? 'Executing…' : 'Approve &amp; execute'}</button>
          <button onClick={onCancel} className="btn btn-ghost flex-1">Cancel</button>
        </div>
      </div>
    </div>
  )
}