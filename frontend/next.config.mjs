/** @type {import('next').NextConfig} */
const nextConfig = {
  // Export estático: `npm run build` genera la carpeta out/ que se sube a Hostinger tal cual.
  output: 'export',
  trailingSlash: true,
  images: { unoptimized: true },
};

export default nextConfig;
