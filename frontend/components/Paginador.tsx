import { Pagina } from '@/lib/api';

export function Paginador<T>({ pagina, onCambiar }: { pagina: Pagina<T>; onCambiar: (p: number) => void }) {
  if (pagina.totalPaginas <= 1) return null;
  return (
    <div className="paginador">
      <span>Página {pagina.pagina + 1} de {pagina.totalPaginas} · {pagina.totalElementos} registros</span>
      <button className="sec" disabled={pagina.pagina === 0} onClick={() => onCambiar(pagina.pagina - 1)}>Anterior</button>
      <button className="sec" disabled={pagina.pagina + 1 >= pagina.totalPaginas} onClick={() => onCambiar(pagina.pagina + 1)}>Siguiente</button>
    </div>
  );
}
