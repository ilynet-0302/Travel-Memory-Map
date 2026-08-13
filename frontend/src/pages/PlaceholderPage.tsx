import { ArrowLeft, Sparkles } from 'lucide-react';
import { Link } from 'react-router-dom';

interface PlaceholderPageProps {
  eyebrow: string;
  title: string;
  description: string;
}

export function PlaceholderPage({ eyebrow, title, description }: PlaceholderPageProps) {
  return (
    <div className="page placeholder-page">
      <span className="placeholder-page__orb"><Sparkles size={30} /></span>
      <span className="eyebrow">{eyebrow}</span>
      <h1>{title}</h1>
      <p>{description}</p>
      <Link className="button button--dark" to="/"><ArrowLeft size={17} /> Back to overview</Link>
    </div>
  );
}
