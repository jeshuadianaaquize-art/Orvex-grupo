import { NextResponse, type NextRequest } from "next/server";

/** La web reenvía /api al gateway (next.config). */
export function middleware(req: NextRequest) {
  const cabeceras = new Headers(req.headers);
  cabeceras.set("ngrok-skip-browser-warning", "1");
  return NextResponse.next({ request: { headers: cabeceras } });
}

export const config = { matcher: "/api/:path*" };
