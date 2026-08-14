export interface TripPhoto {
  id: string;
  tripId: string;
  tripStopId: string | null;
  uploadedByUserId: string;
  uploadedByDisplayName: string;
  storagePath: string;
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

export interface UploadPhotoInput {
  file: File;
  caption?: string;
  tripStopId?: string;
}
