const CLASE: Record<string, string> = { ACTIVO: 'ok', INACTIVO: 'neutro', FALLECIDO: 'bad' };

export function EstadoChip({ estado }: { estado: string }) {
  return <span className={`chip ${CLASE[estado] ?? 'neutro'}`}>{estado}</span>;
}
