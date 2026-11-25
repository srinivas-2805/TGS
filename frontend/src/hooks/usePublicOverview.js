import { useState, useEffect } from 'react';
import api from '../services/api';

let cachedOverview = null;

export function clearPublicOverviewCache() {
  cachedOverview = null;
}

export function usePublicOverview() {
  const [data, setData] = useState(cachedOverview);
  const [loading, setLoading] = useState(!cachedOverview);
  const [error, setError] = useState(null);

  const reload = async () => {
    setLoading(true);
    try {
      const res = await api.get('/public/overview');
      cachedOverview = res.data?.data || {};
      setData(cachedOverview);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to load school information from server.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!cachedOverview) {
      reload();
    }
  }, []);

  const getSection = (key) => {
    const list = data?.content || [];
    return list.find((c) => c.sectionKey === key) || null;
  };

  const parseMeta = (section) => {
    if (!section?.metadataJson) return {};
    try {
      return JSON.parse(section.metadataJson);
    } catch {
      return {};
    }
  };

  return {
    data,
    loading,
    error,
    reload,
    getSection,
    parseMeta,
    announcements: data?.announcements || [],
    events: data?.events || [],
    gallery: data?.gallery || [],
    classes: data?.classes || [],
  };
}
