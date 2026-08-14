const DAY_MS = 86_400_000;

function utcDay(dateKey: string) {
  const [year, month, day] = dateKey.split('-').map(Number);
  return Date.UTC(year, month - 1, day);
}

export function tripDayNumber(dateKey: string, tripStartDate: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(dateKey)) return null;
  return Math.round((utcDay(dateKey) - utcDay(tripStartDate)) / DAY_MS) + 1;
}

export function localDateKeyFromInstant(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value.slice(0, 10);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function formatTripDate(dateKey: string) {
  return new Intl.DateTimeFormat('en-GB', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
  }).format(new Date(`${dateKey}T12:00:00`));
}
