export interface PacienteResumen {
  id: number; historiaClinica: string; dni: string; nombres: string; apellidos: string;
  fechaNacimiento: string; estado: string;
}
export interface Alergia { id: number; principioActivo: string; reaccion: string | null }
export interface PacienteDetalle extends PacienteResumen {
  sexo: string; telefono: string | null; email: string | null; direccion: string | null;
  estadosPermitidos: string[]; alergias: Alergia[];
}
export interface Producto {
  id: number; codigo: string; nombre: string; principioActivo: string; presentacion: string;
  precioVenta: number; costoPromedio: number; stockMinimo: number; activo: boolean;
  stockFisico: number; stockDisponible: number;
}
export interface Lote {
  id: number; numeroLote: string; fechaVencimiento: string; cantidadInicial: number; cantidadActual: number;
  costoUnitario: number; fechaIngreso: string;
}
export interface Movimiento {
  id: number; tipo: 'ENTRADA' | 'SALIDA' | 'AJUSTE'; numeroLote: string | null; cantidad: number; saldo: number;
  costoUnitario: number; referencia: string; usuario: string; fecha: string;
}
export interface AlertaStock { productoId: number; codigo: string; nombre: string; stockDisponible: number; stockMinimo: number }
export interface AlertaLote {
  loteId: number; productoId: number; producto: string; numeroLote: string; fechaVencimiento: string;
  cantidad: number; diasRestantes: number;
}
export interface Alertas { fecha: string; bajoMinimo: AlertaStock[]; porVencer: AlertaLote[]; vencidos: AlertaLote[] }
export interface Valorizacion {
  fecha: string; total: number;
  lineas: { productoId: number; codigo: string; nombre: string; stock: number; costoPromedio: number; valor: number }[];
}
export interface Usuario { id: number; username: string; nombreCompleto: string; rol: string; activo: boolean }
export interface RegistroAuditoria {
  id: number; usuario: string; accion: string; entidad: string; entidadId: string | null; detalle: string | null; fecha: string;
}
