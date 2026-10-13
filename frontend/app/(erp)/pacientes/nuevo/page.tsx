'use client';

import { useRouter } from 'next/navigation';
import { api } from '@/lib/api';
import { PacienteDetalle } from '@/lib/tipos';
import { FORM_VACIO, FormularioPaciente } from '@/components/FormularioPaciente';

export default function NuevoPaciente() {
  const router = useRouter();
  return (
    <>
      <h1>Registrar paciente</h1>
      <p className="sub">La historia clínica se genera automáticamente</p>
      <FormularioPaciente inicial={FORM_VACIO} textoBoton="Registrar" onGuardar={async (f) => {
        const p = await api<PacienteDetalle>('/pacientes', { method: 'POST', json: f });
        router.push(`/pacientes/ficha/?id=${p.id}&nuevo=1`);
      }} />
    </>
  );
}
