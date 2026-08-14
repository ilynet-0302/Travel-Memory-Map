export interface OnThisDaySpending {
  currency: string;
  totalSpent: number;
}

export interface OnThisDayMemory {
  tripId: string;
  tripTitle: string;
  country: string;
  countryCode: string;
  city: string;
  memoryDate: string;
  yearsAgo: number;
  placeCount: number;
  photoCount: number;
  spending: OnThisDaySpending[];
  heroPhotoUrl: string | null;
  heroPhotoCaption: string | null;
  placeNames: string[];
}

export interface OnThisDayData {
  date: string;
  memories: OnThisDayMemory[];
}

export interface MemoryPhoto {
  id: string;
  tripId: string;
  tripTitle: string;
  country: string;
  countryCode: string;
  city: string;
  tripStartDate: string;
  tripEndDate: string;
  tripStopId: string | null;
  tripStopName: string | null;
  uploadedByUserId: string;
  uploadedByDisplayName: string;
  signedUrl: string;
  originalFileName: string;
  contentType: string;
  fileSize: number;
  takenAt: string | null;
  latitude: number | null;
  longitude: number | null;
  caption: string | null;
  publicVisible: boolean;
  createdAt: string;
}

export interface MemoryGalleryFilters {
  query: string;
  year: string;
  tripId: string;
  locationOnly: boolean;
}
