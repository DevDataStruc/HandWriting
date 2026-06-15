import router from './routes'
import { setupGuards } from './guards'

setupGuards(router)

export default router
export * from './routes'
