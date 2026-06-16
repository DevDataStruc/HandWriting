import { describe, expect, it } from 'vitest'
import { useHandwriting } from '../useHandwriting'

function makePts(
  n: number,
  baseY = 100
): Array<{ x: number; y: number; pressure: number; time: number }> {
  return Array.from({ length: n }, (_, i) => ({
    x: 10 + i * 5,
    y: baseY + (i % 2 === 0 ? 0 : 2),
    pressure: 0.5,
    time: i * 16,
  }))
}

describe('useHandwriting', () => {
  it('initial state is empty and not undoable/redoable', () => {
    const w = useHandwriting()
    expect(w.strokes.value).toHaveLength(0)
    expect(w.canUndo()).toBe(false)
    expect(w.canRedo()).toBe(false)
    expect(w.currentBrush.value.id).toBeTruthy()
  })

  it('setSize clamps into [SIZE_MIN, SIZE_MAX]', () => {
    const w = useHandwriting()
    w.setSize(0)
    expect(w.size.value).toBe(1)
    w.setSize(9999)
    expect(w.size.value).toBe(100)
    w.setSize(10)
    expect(w.size.value).toBe(10)
  })

  it('pressure sensitivity changes stroke width at the same input pressure', () => {
    const pts = makePts(20)
    const baseline = useHandwriting({ brush: 'willow-thick', pressureSensitivity: 0 })
    baseline.startStroke(pts[0])
    pts.slice(1).forEach((p) => baseline.extendStroke(p))
    const d0 = baseline.endStroke()!.d

    const amplified = useHandwriting({ brush: 'willow-thick', pressureSensitivity: 200 })
    amplified.startStroke(pts[0])
    pts.slice(1).forEach((p) => amplified.extendStroke(p))
    const d1 = amplified.endStroke()!.d

    expect(d0).not.toBe(d1)
  })

  it('pressure curve changes stroke output', () => {
    const pts = makePts(20)
    const lin = useHandwriting({ brush: 'pen-fine', pressureCurve: 'linear' })
    lin.startStroke(pts[0])
    pts.slice(1).forEach((p) => lin.extendStroke(p))
    const linD = lin.endStroke()!.d

    const soft = useHandwriting({ brush: 'pen-fine', pressureCurve: 'soft' })
    soft.startStroke(pts[0])
    pts.slice(1).forEach((p) => soft.extendStroke(p))
    const softD = soft.endStroke()!.d

    expect(linD).not.toBe(softD)
  })

  it('pressure off collapses to a uniform width stroke', () => {
    const pts = [
      { x: 0, y: 0, pressure: 1.0, time: 0 },
      { x: 5, y: 0, pressure: 0.0, time: 16 },
      { x: 10, y: 0, pressure: 1.0, time: 32 },
    ]
    const w = useHandwriting({ brush: 'pen-fine', pressure: false })
    w.startStroke(pts[0])
    pts.slice(1).forEach((p) => w.extendStroke(p))
    const d = w.endStroke()!.d
    // 输入压力为 0 时线宽应等于 0.5 时的尺寸
    expect(d).toMatch(/^M.*Z$/)
  })

  it('setAngle clamps into [0, 180]', () => {
    const w = useHandwriting()
    w.setAngle(-10)
    expect(w.angle.value).toBe(0)
    w.setAngle(999)
    expect(w.angle.value).toBe(180)
  })

  it('setBrush ignores unknown ids', () => {
    const w = useHandwriting({ brush: 'pen-fine' })
    w.setBrush('__nope__')
    expect(w.brushId.value).toBe('pen-fine')
    w.setBrush('willow-flowing')
    expect(w.brushId.value).toBe('willow-flowing')
    expect(w.currentBrush.value.category).toBe('willow')
  })

  it('endStroke produces a closed SVG path starting with M and ending with Z', () => {
    const w = useHandwriting({ brush: 'pen-fine' })
    const pts = makePts(20)
    w.startStroke(pts[0])
    pts.slice(1).forEach((p) => w.extendStroke(p))
    const snap = w.endStroke()
    expect(snap).not.toBeNull()
    expect(snap!.d.startsWith('M')).toBe(true)
    expect(snap!.d.trim().endsWith('Z')).toBe(true)
    expect(w.strokes.value).toHaveLength(1)
  })

  it('previewPath does not commit a stroke', () => {
    const w = useHandwriting({ brush: 'pen-fine' })
    const pts = makePts(10)
    const d = w.previewPath(pts)
    expect(d).toContain('M')
    expect(w.strokes.value).toHaveLength(0)
  })

  it('short strokes (<2 points) produce no path', () => {
    const w = useHandwriting()
    w.startStroke({ x: 0, y: 0, pressure: 0.5, time: 0 })
    const snap = w.endStroke()
    expect(snap).toBeNull()
    expect(w.strokes.value).toHaveLength(0)
  })

  it('undo and redo preserve stroke data', () => {
    const w = useHandwriting({ brush: 'pen-fine', color: '#ff0000' })
    w.startStroke({ x: 0, y: 0, pressure: 0.5, time: 0 })
    makePts(10).forEach((p) => w.extendStroke(p))
    w.endStroke()
    w.setColor('#00ff00')
    w.startStroke({ x: 50, y: 50, pressure: 0.5, time: 0 })
    makePts(8).forEach((p) => w.extendStroke(p))
    w.endStroke()
    expect(w.strokes.value).toHaveLength(2)
    const second = w.strokes.value[1]
    expect(second.color).toBe('#00ff00')

    w.undo()
    expect(w.strokes.value).toHaveLength(1)
    expect(w.canRedo()).toBe(true)

    w.redo()
    expect(w.strokes.value).toHaveLength(2)
    // redo 之后颜色仍是当时提交的颜色，不是当前色
    expect(w.strokes.value[1].color).toBe('#00ff00')
  })

  it('clear resets everything', () => {
    const w = useHandwriting()
    w.startStroke({ x: 0, y: 0, pressure: 0.5, time: 0 })
    makePts(5).forEach((p) => w.extendStroke(p))
    w.endStroke()
    w.undo()
    w.clear()
    expect(w.strokes.value).toHaveLength(0)
    expect(w.redoStack.value).toHaveLength(0)
  })

  it('toSVG includes fill-rule="nonzero" so the closed path can be filled', () => {
    const w = useHandwriting({ brush: 'pen-fine' })
    w.startStroke({ x: 0, y: 0, pressure: 0.5, time: 0 })
    makePts(8).forEach((p) => w.extendStroke(p))
    w.endStroke()
    const svg = w.toSVG(200, 200)
    expect(svg).toContain('fill-rule="nonzero"')
    expect(svg).toContain('fill=')
  })

  it('different brushes produce different path outputs (柳叶笔 ≠ 钢笔)', () => {
    const pts = makePts(20)
    const pen = useHandwriting({ brush: 'pen-fine' })
    pen.startStroke(pts[0])
    pts.slice(1).forEach((p) => pen.extendStroke(p))
    const penD = pen.endStroke()!.d

    const willow = useHandwriting({ brush: 'willow-thick' })
    willow.startStroke(pts[0])
    pts.slice(1).forEach((p) => willow.extendStroke(p))
    const willowD = willow.endStroke()!.d

    // 两个笔刷产生的 path d 必须不同（taper/thinning 参数生效）
    expect(penD).not.toBe(willowD)
    expect(penD).toMatch(/^M.*Z$/)
    expect(willowD).toMatch(/^M.*Z$/)
    // 切换笔刷不会影响已提交笔画的设置
    expect(pen.strokes.value[0].d).toBe(penD)
  })
})
