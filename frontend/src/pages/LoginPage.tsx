import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowRight, Eye, EyeOff, Globe2, LockKeyhole, Mail, MapPin, Sparkles } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { useAuth } from '../features/auth/context/AuthContext';
import { buildAuthRedirectUrl } from '../features/auth/authRedirect';

const signInSchema = z.object({
  email: z.email('Enter a valid email address.'),
  password: z.string().min(8, 'Password must have at least 8 characters.'),
});

const signUpSchema = signInSchema.extend({
  name: z.string().trim().min(2, 'Tell us what to call you.'),
});

type SignInForm = z.infer<typeof signInSchema>;
type SignUpForm = z.infer<typeof signUpSchema>;

export function LoginPage() {
  const { demoMode, session, signIn, signUp } = useAuth();
  const [mode, setMode] = useState<'sign-in' | 'sign-up'>('sign-in');
  const [showPassword, setShowPassword] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [confirmationSent, setConfirmationSent] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const destination = (location.state as { from?: string } | null)?.from ?? '/';
  const signInForm = useForm<SignInForm>({ resolver: zodResolver(signInSchema) });
  const signUpForm = useForm<SignUpForm>({ resolver: zodResolver(signUpSchema) });

  if (session) return <Navigate to={destination} replace />;

  const submitSignIn = signInForm.handleSubmit(async (values) => {
    setFormError(null);
    try {
      await signIn(values.email, values.password);
      navigate(destination, { replace: true });
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Unable to sign in.');
    }
  });

  const submitSignUp = signUpForm.handleSubmit(async (values) => {
    setFormError(null);
    try {
      const result = await signUp(values.name, values.email, values.password, buildAuthRedirectUrl(destination));
      if (result === 'confirmation-required') setConfirmationSent(true);
      else navigate(destination, { replace: true });
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Unable to create your account.');
    }
  });

  return (
    <main className="login-page">
      <section className="login-story">
        <div className="login-brand">
          <span><Globe2 size={23} /></span>
          <strong>Travel <small>memory map</small></strong>
        </div>
        <div className="login-story__copy">
          <span className="eyebrow eyebrow--light">YOUR STORY, MAPPED</span>
          <h1>Go somewhere.<br /><em>Remember everything.</em></h1>
          <p>Turn the places you visit into a living story—one route, memory and shared moment at a time.</p>
        </div>
        <div className="login-route" aria-hidden="true">
          <span className="login-route__line" />
          <span className="login-route__pin login-route__pin--one"><MapPin size={19} /></span>
          <span className="login-route__pin login-route__pin--two"><Sparkles size={16} /></span>
          <span className="login-route__pin login-route__pin--three"><MapPin size={19} /></span>
        </div>
        <blockquote>“A journey is best measured in friends, rather than miles.”</blockquote>
      </section>

      <section className="login-form-panel">
        <div className="login-form-card">
          <span className="eyebrow">WELCOME, TRAVELLER</span>
          <h2>{mode === 'sign-in' ? 'Your memories are waiting.' : 'Start your travel story.'}</h2>
          <p>{mode === 'sign-in' ? 'Sign in to continue exploring your world.' : 'Create an account to map your first journey.'}</p>

          {demoMode && (
            <div className="demo-notice">
              <Sparkles size={17} />
              <span><strong>Demo mode is active</strong> Explore the current product slice without an account.</span>
              <button type="button" onClick={() => navigate('/')}>Open demo <ArrowRight size={15} /></button>
            </div>
          )}

          <div className="auth-tabs" role="tablist">
            <button type="button" className={mode === 'sign-in' ? 'is-active' : ''} onClick={() => setMode('sign-in')}>Sign in</button>
            <button type="button" className={mode === 'sign-up' ? 'is-active' : ''} onClick={() => setMode('sign-up')}>Create account</button>
          </div>

          {confirmationSent ? (
            <div className="confirmation-state">
              <span><Mail size={25} /></span>
              <h3>Check your inbox</h3>
              <p>Confirm your email and we will return you to the page where you started.</p>
              <button className="button button--dark" type="button" onClick={() => { setConfirmationSent(false); setMode('sign-in'); }}>Back to sign in</button>
            </div>
          ) : mode === 'sign-in' ? (
            <form className="auth-form" onSubmit={submitSignIn}>
              <label className="field">
                <span>Email address</span>
                <span className="input-with-icon"><Mail size={17} /><input type="email" placeholder="you@example.com" {...signInForm.register('email')} /></span>
                {signInForm.formState.errors.email && <small className="field__error">{signInForm.formState.errors.email.message}</small>}
              </label>
              <label className="field">
                <span>Password</span>
                <span className="input-with-icon"><LockKeyhole size={17} /><input type={showPassword ? 'text' : 'password'} placeholder="At least 8 characters" {...signInForm.register('password')} /><button type="button" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? 'Hide password' : 'Show password'}>{showPassword ? <EyeOff size={17} /> : <Eye size={17} />}</button></span>
                {signInForm.formState.errors.password && <small className="field__error">{signInForm.formState.errors.password.message}</small>}
              </label>
              {formError && <p className="form-error">{formError}</p>}
              <button className="button button--coral button--full" type="submit" disabled={signInForm.formState.isSubmitting}>Continue <ArrowRight size={16} /></button>
            </form>
          ) : (
            <form className="auth-form" onSubmit={submitSignUp}>
              <label className="field"><span>Your name</span><input placeholder="Iliya Petrov" {...signUpForm.register('name')} />{signUpForm.formState.errors.name && <small className="field__error">{signUpForm.formState.errors.name.message}</small>}</label>
              <label className="field"><span>Email address</span><span className="input-with-icon"><Mail size={17} /><input type="email" placeholder="you@example.com" {...signUpForm.register('email')} /></span>{signUpForm.formState.errors.email && <small className="field__error">{signUpForm.formState.errors.email.message}</small>}</label>
              <label className="field"><span>Password</span><span className="input-with-icon"><LockKeyhole size={17} /><input type={showPassword ? 'text' : 'password'} placeholder="At least 8 characters" {...signUpForm.register('password')} /><button type="button" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? 'Hide password' : 'Show password'}>{showPassword ? <EyeOff size={17} /> : <Eye size={17} />}</button></span>{signUpForm.formState.errors.password && <small className="field__error">{signUpForm.formState.errors.password.message}</small>}</label>
              {formError && <p className="form-error">{formError}</p>}
              <button className="button button--coral button--full" type="submit" disabled={signUpForm.formState.isSubmitting}>Create my map <ArrowRight size={16} /></button>
            </form>
          )}
          <small className="login-terms">By continuing, you agree to keep other travellers’ private memories private.</small>
        </div>
      </section>
    </main>
  );
}
