/** La web reenvía /api/* al API Gateway desde el servidor de Next.js. */
const gateway = process.env.API_INTERNA_URL ?? "http://localhost:8080";

/** @type {import('next').NextConfig} */
const nextConfig = {
  async rewrites() {
    return [{ source: "/api/:ruta*", destination: `${gateway}/api/:ruta*` }];
  },
};
export default nextConfig;
