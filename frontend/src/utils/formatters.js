export function formatCurrency(amount) {
  if (amount === null || amount === undefined || amount === '') return '₹0.00';
  const num = Number(amount);
  if (Number.isNaN(num)) return '₹0.00';
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(num);
}

export function formatDate(val) {
  if (!val) return '-';
  const d = new Date(val);
  if (Number.isNaN(d.getTime())) return String(val);
  return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

export function formatDateTime(val) {
  if (!val) return '-';
  const d = new Date(val);
  if (Number.isNaN(d.getTime())) return String(val);
  return d.toLocaleString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export const ROLE_LABELS = {
  SUPER_ADMIN: 'Super Admin',
  PRINCIPAL: 'Principal',
  ADMIN: 'Administrator',
  TEACHER: 'Teacher',
  ACCOUNTANT: 'Accountant',
  PARENT: 'Parent',
  STUDENT: 'Student',
  LIBRARIAN: 'Librarian',
};
