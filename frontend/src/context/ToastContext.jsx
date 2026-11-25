import React, { createContext, useContext, useState, useCallback } from 'react';
import { CheckCircle2, AlertCircle, Info, X } from 'lucide-react';

const ToastContext = createContext(null);

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback(
    (message, type = 'success') => {
      const id = Date.now() + Math.random();
      setToasts((prev) => [...prev, { id, message, type }]);
      setTimeout(() => removeToast(id), 4500);
    },
    [removeToast]
  );

  const toast = {
    success: (msg) => showToast(msg, 'success'),
    error: (msg) => showToast(msg, 'danger'),
    info: (msg) => showToast(msg, 'info'),
    warning: (msg) => showToast(msg, 'warning'),
  };

  return (
    <ToastContext.Provider value={toast}>
      {children}
      <div
        className="position-fixed top-0 end-0 p-3 d-flex flex-column gap-2 no-print"
        style={{ zIndex: 1090, maxWidth: '380px' }}
      >
        {toasts.map((t) => (
          <div
            key={t.id}
            role="alert"
            className={`alert alert-${t.type} shadow-sm border-0 d-flex align-items-start gap-2 mb-0 py-2 px-3`}
          >
            {t.type === 'success' && <CheckCircle2 size={18} className="mt-1 flex-shrink-0" />}
            {t.type === 'danger' && <AlertCircle size={18} className="mt-1 flex-shrink-0" />}
            {(t.type === 'info' || t.type === 'warning') && <Info size={18} className="mt-1 flex-shrink-0" />}
            <div className="small flex-grow-1 fw-medium">{t.message}</div>
            <button
              type="button"
              className="btn-close btn-sm"
              aria-label="Close"
              onClick={() => removeToast(t.id)}
            />
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used inside ToastProvider');
  return ctx;
}
