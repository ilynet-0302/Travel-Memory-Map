interface AvatarStackProps {
  count: number;
  compact?: boolean;
}

const people = [
  { initials: 'IP', className: 'avatar--ilia' },
  { initials: 'MA', className: 'avatar--maria' },
  { initials: 'IV', className: 'avatar--ivan' },
];

export function AvatarStack({ count, compact = false }: AvatarStackProps) {
  const visible = people.slice(0, Math.min(count, people.length));

  return (
    <div className={`avatar-stack${compact ? ' avatar-stack--compact' : ''}`} aria-label={`${count} trip members`}>
      {visible.map((person) => (
        <span key={person.initials} className={`avatar ${person.className}`} aria-hidden="true">
          {person.initials}
        </span>
      ))}
      {count > people.length && <span className="avatar avatar--more">+{count - people.length}</span>}
    </div>
  );
}
