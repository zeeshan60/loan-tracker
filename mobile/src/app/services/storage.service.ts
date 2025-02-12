import { Injectable } from '@angular/core';
import { Storage } from '@ionic/storage-angular';
import { AsyncSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class StorageService {
  private _storage: Storage | null = null;
  public storageReady$: AsyncSubject<boolean> = new AsyncSubject();

  constructor(private storage: Storage) {
    this.init();
  }

  async init() {
    this._storage = await this.storage.create();
    this.storageReady$.next(true);
    this.storageReady$.complete();
  }

  public set(key: string, value: any) {
    return this._storage?.set(key, value);
  }

  public get(key: string) {
    return this._storage?.get(key);
  }

  public remove(key: string) {
    return this._storage?.remove(key);
  }

  public clear() {
    return this._storage?.clear();
  }
}
