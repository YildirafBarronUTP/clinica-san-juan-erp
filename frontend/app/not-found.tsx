import Link from 'next/link';

export default function NoEncontrado() {
  return (
    <div className="login">
      <div className="card">
        <h1>Página no encontrada</h1>
        <p className="sub">La dirección no existe en el ERP.</p>
        <Link className="boton" href="/dashboard/">Ir al inicio</Link>
      </div>
    </div>
  );
}
