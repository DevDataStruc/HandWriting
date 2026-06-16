/**
 * 开发环境 mock 入口
 */
import type { MockMethod } from 'vite-plugin-mock'

const mocks: MockMethod[] = [
  {
    url: '/v1/auth/login',
    method: 'post',
    timeout: 300,
    response: () => ({
      code: 0,
      msg: 'ok',
      data: {
        token: 'mock-jwt-token-' + Date.now(),
        refreshToken: 'mock-refresh-token',
        expiresIn: 7200,
      },
    }),
  },
  {
    url: '/v1/user/profile',
    method: 'get',
    response: () => ({
      code: 0,
      msg: 'ok',
      data: {
        id: 1001,
        username: 'demo_user',
        nickname: '示例用户',
        email: 'demo@example.com',
        phone: '138****8888',
        avatar: '',
        gender: 1,
        bio: '热爱书法的 AI 数据贡献者',
        roles: ['USER'],
        permissions: ['sample:upload', 'sample:list'],
        createdAt: '2025-12-01 10:00:00',
        lastLoginAt: '2026-06-15 09:00:00',
        status: 'active',
        sampleCount: 128,
      },
    }),
  },
  {
    url: '/v1/dict/random',
    method: 'get',
    response: () => ({
      code: 0,
      msg: 'ok',
      data: {
        id: Math.floor(Math.random() * 1000),
        char: '永',
        pinyin: 'yǒng',
        radical: '水',
        strokeCount: 5,
        category: '常用字',
        difficulty: 3,
        sampleCount: 0,
        targetCount: 100,
      },
    }),
  },
  {
    url: '/v1/stats/overview',
    method: 'get',
    response: () => ({
      code: 0,
      msg: 'ok',
      data: {
        totalSamples: 12580,
        totalUsers: 326,
        todaySamples: 142,
        todayUsers: 12,
        pendingAudits: 86,
        approvedSamples: 11200,
        rejectedSamples: 1294,
        activeChars: 1850,
        growthRate: 12.4,
      },
    }),
  },
  {
    url: '/v1/stats/trend',
    method: 'get',
    response: () => {
      const days = 30
      const dates: string[] = []
      const samples: number[] = []
      const users: number[] = []
      const today = new Date()
      for (let i = days - 1; i >= 0; i--) {
        const d = new Date(today.getTime() - i * 86400_000)
        dates.push(`${d.getMonth() + 1}/${d.getDate()}`)
        samples.push(Math.floor(Math.random() * 200) + 50)
        users.push(Math.floor(Math.random() * 20) + 2)
      }
      return { code: 0, msg: 'ok', data: { dates, samples, users } }
    },
  },
  {
    url: '/v1/stats/status-distribution',
    method: 'get',
    response: () => ({
      code: 0,
      msg: 'ok',
      data: [
        { status: 'PENDING', count: 86 },
        { status: 'APPROVED', count: 11200 },
        { status: 'REJECTED', count: 1294 },
      ],
    }),
  },
  {
    url: '/v1/stats/top-contributors',
    method: 'get',
    response: () => ({
      code: 0,
      msg: 'ok',
      data: [
        {
          userId: 1,
          username: 'calligrapher',
          nickname: '书法家A',
          sampleCount: 532,
          approvedCount: 510,
        },
        { userId: 2, username: 'writer', nickname: '小编', sampleCount: 412, approvedCount: 400 },
        { userId: 3, username: 'demo', nickname: 'Demo', sampleCount: 320, approvedCount: 305 },
        { userId: 4, username: 'pen', nickname: '钢笔侠', sampleCount: 280, approvedCount: 250 },
        {
          userId: 5,
          username: 'student',
          nickname: '小学生',
          sampleCount: 210,
          approvedCount: 200,
        },
      ],
    }),
  },
  {
    url: '/v1/audit/pending',
    method: 'get',
    response: () => ({
      code: 0,
      msg: 'ok',
      data: {
        list: Array.from({ length: 8 }, (_, i) => ({
          id: i + 1,
          charId: i + 1,
          char: '永' /* placeholder */,
          imageUrl: '',
          userId: 1,
          username: 'demo_user',
          status: 'PENDING',
          strokeCount: 5,
          duration: 12000 + i * 1000,
          createdAt: '2026-06-15 09:00:00',
        })),
        total: 86,
        pageNum: 1,
        pageSize: 10,
      },
    }),
  },
  {
    url: '/v1/sample/page',
    method: 'get',
    response: () => ({
      code: 0,
      msg: 'ok',
      data: {
        list: [],
        total: 0,
        pageNum: 1,
        pageSize: 10,
      },
    }),
  },
  {
    url: '/v1/sample/upload',
    method: 'post',
    timeout: 400,
    response: () => ({
      code: 0,
      msg: 'ok',
      data: {
        id: Math.floor(Math.random() * 100000) + 1000,
        url: '',
        charId: 1,
        createdAt: new Date().toISOString(),
      },
    }),
  },
  {
    url: '/v1/sample/:id',
    method: 'delete',
    response: () => ({ code: 0, msg: 'ok', data: null }),
  },
  {
    url: '/v1/sample/batch-delete',
    method: 'post',
    response: () => ({ code: 0, msg: 'ok', data: null }),
  },
]

export default mocks
