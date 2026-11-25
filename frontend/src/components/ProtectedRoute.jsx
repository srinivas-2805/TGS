import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { LoadingSkeleton } from './CommonUI';

export default function ProtectedRoute({ allowedRoles, children }) {
  const { isAuthenticated, loading, hasAnyRole, defaultRoute } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="container py-5">
        <LoadingSkeleton rows={5} />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && allowedRoles.length > 0 && !hasAnyRole(...allowedRoles)) {
    return <Navigate to={defaultRoute} replace />;
  }

  return children;
}
