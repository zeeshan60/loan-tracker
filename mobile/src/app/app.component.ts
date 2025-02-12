import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { IonApp, IonRouterOutlet, LoadingController } from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  ellipse,
  logoGoogle,
  square,
  triangle,
  peopleOutline,
  settingsOutline,
  addCircleOutline,
  logOutOutline,
  addOutline,
} from 'ionicons/icons';
import { signalMethod } from '@ngrx/signals';
import { FriendsStore } from './friends/friends.store';
import { IonicStorageModule } from '@ionic/storage-angular';
import { AuthStore } from './login/auth.store';
import { StorageService } from './services/storage.service';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html',
  standalone: true,
  imports: [
    IonApp,
    IonRouterOutlet,
    IonicStorageModule
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent implements OnInit {
  readonly loadingCtrl = inject(LoadingController);
  readonly authStore = inject(AuthStore);
  readonly storageService = inject(StorageService);
  private loader: HTMLIonLoadingElement | null = null;

  readonly activateLoaderWhen = signalMethod<boolean>(async (isLoading) => {
    if (!this.loader && isLoading) {
      this.loader = await this.loadingCtrl.create({
        duration: 2000,
      });
    }
    if (isLoading) {
      this.loader?.present();
    } else {
      this.loader?.dismiss();
      this.loader = null;
    }
  });

  readonly friendsStore = inject(FriendsStore);

  constructor() {
    addIcons({
      triangle,
      ellipse,
      square,
      logoGoogle,
      peopleOutline,
      settingsOutline,
      addCircleOutline,
      logOutOutline,
      addOutline,
    });

    this.activateLoaderWhen(this.friendsStore.loading);
  }

  async ngOnInit() {
    await this.initApp();
  }
  async initApp() {
    this.storageService.storageReady$.subscribe(async () => {
      await this.authStore.setApiKey();
    });
  }
}
