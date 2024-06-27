import axios from 'axios'

const baseURL = 'http://localhost:8893'

const axiosIns = axios.create({
  baseURL: baseURL,
  timeout: 1000000,
})

axiosIns.interceptors.request.use(
  config => {
    config.headers['Access-Control-Allow-Origin'] = '*'
    config.headers['Access-Control-Allow-Headers'] = 'Authorization,Origin, X-Requested-With, Content-Type, Accept'
    config.headers['Access-Control-Allow-Methods'] = '*'
    config.headers.withCredentials = true
    return config
  },
  error => Promise.reject(error),
)

axiosIns.interceptors.response.use(
  response => {
    // 如果返回的状态码为200，说明成功，可以直接使用数据
    if (response.status === 200) {
      return response;
    } else {
      // 其他状态码都当作错误处理
      return Promise.reject(response);
    }
  },
  error => {
    return Promise.reject(error)
  },
)

export default axiosIns
