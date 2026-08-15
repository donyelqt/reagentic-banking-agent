import type { Step } from '../types'

export default function ApprovalModal({
  steps,
  onApprove,
  onCancel
}: {
  steps: Step[]
  onApprove: () => void
  onCancel: () => void
}) {
  return (
    <div className="border-t bg-amber-50 p-3">
      <p className="font-semibold text-amber-800">Confirmation required</p>
      <ul className="text-sm my-2 space-y-1">
        {steps.map((s) => (
          <li key={s.stepId} className="font-mono text-xs">
            {s.tool} {JSON.stringify(s.args)}
          </li>
        ))}
      </ul>
      <div className="flex gap-2">
        <button className="bg-green-600 text-white rounded px-3 py-1 text-sm" onClick={onApprove}>
          Approve &amp; execute
        </button>
        <button className="border rounded px-3 py-1 text-sm" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </div>
  )
}
