import type { Session } from '@supabase/supabase-js';
import { createContext, useContext, useEffect, useMemo, useState, type PropsWithChildren } from 'react';
import { supabase } from '../../../services/supabase';

interface AuthContextValue {
  session: Session | null;
  loading: boolean;
  demoMode: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (name: string, email: string, password: string, emailRedirectTo?: string) => Promise<'signed-in' | 'confirmation-required'>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);
const demoMode = import.meta.env.VITE_DEMO_MODE !== 'false';

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(!demoMode && Boolean(supabase));

  useEffect(() => {
    if (demoMode || !supabase) {
      return;
    }

    void supabase.auth.getSession().then(({ data }) => {
      setSession(data.session);
      setLoading(false);
    });
    const { data } = supabase.auth.onAuthStateChange((_event, nextSession) => {
      setSession(nextSession);
      setLoading(false);
    });
    return () => data.subscription.unsubscribe();
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      loading,
      demoMode,
      signIn: async (email, password) => {
        if (!supabase) throw new Error('Supabase is not configured.');
        const { error } = await supabase.auth.signInWithPassword({ email, password });
        if (error) throw error;
      },
      signUp: async (name, email, password, emailRedirectTo) => {
        if (!supabase) throw new Error('Supabase is not configured.');
        const { data, error } = await supabase.auth.signUp({
          email,
          password,
          options: {
            data: { full_name: name },
            ...(emailRedirectTo ? { emailRedirectTo } : {}),
          },
        });
        if (error) throw error;
        return data.session ? 'signed-in' : 'confirmation-required';
      },
      signOut: async () => {
        if (!supabase) return;
        const { error } = await supabase.auth.signOut({ scope: 'local' });
        if (error) throw error;
      },
    }),
    [loading, session],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth must be used inside AuthProvider.');
  return value;
}
