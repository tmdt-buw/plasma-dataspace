/**
 * Upload data model
 */
export class Upload {
  file: File;
  loaded: number;
  progress: number;
  upload: any;
  status: 'queued' | 'finished' | 'error' | 'canceled';
  error: string;

  constructor(file: File) {
    this.file = file;
    this.progress = 0;
    this.loaded = 0;
    this.status = 'queued';
  }
}
