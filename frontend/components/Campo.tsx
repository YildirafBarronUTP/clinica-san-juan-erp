interface Props extends React.InputHTMLAttributes<HTMLInputElement> {
  etiqueta: string;
  nombre: string;
  error?: string;
}

export function Campo({ etiqueta, nombre, error, ...input }: Props) {
  return (
    <div className="campo">
      <label htmlFor={nombre}>{etiqueta}</label>
      <input id={nombre} name={nombre} aria-invalid={error ? 'true' : 'false'}
             aria-describedby={error ? `${nombre}-error` : undefined} {...input} />
      {error && <div className="error" id={`${nombre}-error`} data-testid={`error-${nombre}`}>{error}</div>}
    </div>
  );
}
