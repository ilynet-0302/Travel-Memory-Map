const dateFormatter = new Intl.DateTimeFormat('en-GB', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
});

export function formatDateRange(startDate: string, endDate: string) {
  const start = new Date(`${startDate}T12:00:00`);
  const end = new Date(`${endDate}T12:00:00`);

  if (start.getFullYear() === end.getFullYear() && start.getMonth() === end.getMonth()) {
    const monthYear = new Intl.DateTimeFormat('en-GB', {
      month: 'short',
      year: 'numeric',
    }).format(end);
    return `${start.getDate()}–${end.getDate()} ${monthYear}`;
  }

  return `${dateFormatter.format(start)} – ${dateFormatter.format(end)}`;
}

export function tripDuration(startDate: string, endDate: string) {
  const milliseconds = new Date(endDate).getTime() - new Date(startDate).getTime();
  return Math.floor(milliseconds / 86_400_000) + 1;
}
