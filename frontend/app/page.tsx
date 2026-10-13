'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth';

export default function Inicio() {
  const { sesion, cargando } = useAuth();
  const router = useRouter();
  useEffect(() => {
    if (!cargando) router.replace(sesion ? '/dashboard/' : '/login/');
  }, [sesion, cargando, router]);
  return null;
}
