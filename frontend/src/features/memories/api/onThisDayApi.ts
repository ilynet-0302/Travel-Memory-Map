import { apiClient } from '../../../services/apiClient';
import { tripsApi } from '../../trips/api/tripsApi';
import type { Trip, TripStop } from '../../trips/types';
import type { OnThisDayData, OnThisDayMemory } from '../types';

const demoMode = import.meta.env.VITE_DEMO_MODE !== 'false';

function localIsoDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function validIsoDate(value: string) {
  const parsed = new Date(`${value}T00:00:00Z`);
  return !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value;
}

function matchingMemoryDate(trip: Trip, today: string) {
  const monthAndDay = today.slice(4);
  const earliestYear = Number(trip.startDate.slice(0, 4));
  for (let year = Number(today.slice(0, 4)) - 1; year >= earliestYear; year -= 1) {
    const candidate = `${year}${monthAndDay}`;
    if (validIsoDate(candidate) && candidate >= trip.startDate && candidate <= trip.endDate) return candidate;
  }
  return null;
}

function stopDate(trip: Trip, stop: TripStop) {
  if (stop.arrivalAt) return stop.arrivalAt.slice(0, 10);
  const date = new Date(`${trip.startDate}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + stop.day - 1);
  return date.toISOString().slice(0, 10);
}

function demoMemory(trip: Trip, memoryDate: string, today: string): OnThisDayMemory {
  const stops = trip.stops.filter((stop) => stopDate(trip, stop) === memoryDate);
  return {
    tripId: trip.id,
    tripTitle: trip.title,
    country: trip.country,
    countryCode: trip.countryCode,
    city: trip.city,
    memoryDate,
    yearsAgo: Number(today.slice(0, 4)) - Number(memoryDate.slice(0, 4)),
    placeCount: stops.length,
    photoCount: 0,
    spending: [],
    heroPhotoUrl: null,
    heroPhotoCaption: null,
    placeNames: stops.map((stop) => stop.name),
  };
}

async function demoOnThisDay(): Promise<OnThisDayData> {
  const today = localIsoDate(new Date());
  const trips = await tripsApi.list();
  const memories = trips
    .map((trip) => {
      const memoryDate = matchingMemoryDate(trip, today);
      return memoryDate ? demoMemory(trip, memoryDate, today) : null;
    })
    .filter((memory): memory is OnThisDayMemory => memory !== null)
    .sort((left, right) => left.yearsAgo - right.yearsAgo || left.tripTitle.localeCompare(right.tripTitle));
  return { date: today, memories };
}

export const onThisDayApi = {
  async get(): Promise<OnThisDayData> {
    if (!demoMode) return apiClient<OnThisDayData>('/profile/on-this-day');
    return demoOnThisDay();
  },
};
