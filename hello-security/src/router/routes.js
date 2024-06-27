export default [
    {
        path: '/fuck',
        name: "fuck",
        component: () => import('@/views/Fuck.vue')
    },
    {
        path: '/',
        component: () => import('@/components/HelloWorld.vue')
    },
]