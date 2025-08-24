import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { IonApp, IonRouterOutlet, Platform } from '@ionic/angular/standalone';
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
  person,
  mailOutline,
  callOutline,
  listOutline,
  receiptOutline,
  trashOutline,
  createOutline,
  closeCircle,
  checkmarkOutline,
  chevronDownOutline,
  closeOutline,
  ellipsisVerticalOutline,
  arrowForwardOutline,
  create
} from 'ionicons/icons';
import { FriendsStore } from './friends/friends.store';
import { IonicStorageModule } from '@ionic/storage-angular';
import { AuthStore } from './login/auth.store';
import { StorageService } from './services/storage.service';
import { SafeArea } from 'capacitor-plugin-safe-area';
import { JsonPipe } from '@angular/common';
import { isAndroid, platform } from './utils';

@Component({
  selector: 'mr-root',
  templateUrl: 'app.component.html',
  standalone: true,
  imports: [
    IonApp,
    IonRouterOutlet,
    IonicStorageModule,
    JsonPipe
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent implements OnInit {
  readonly authStore = inject(AuthStore);
  readonly storageService = inject(StorageService);
  readonly friendsStore = inject(FriendsStore);
  readonly platform = inject(Platform);
  insets = signal({});

  constructor() {
    addIcons({
      create,
      triangle,
      ellipse,
      square,
      logoGoogle,
      peopleOutline,
      settingsOutline,
      addCircleOutline,
      logOutOutline,
      addOutline,
      person,
      mailOutline,
      callOutline,
      listOutline,
      receiptOutline,
      trashOutline,
      createOutline,
      closeCircle,
      checkmarkOutline,
      chevronDownOutline,
      closeOutline,
      ellipsisVerticalOutline,
      arrowForwardOutline
    });
  }

  async ngOnInit() {
    await this.initApp();
  }
  async initApp() {

    await this.platform.ready();

    if (isAndroid) {
      await this.setSafeAreaInsects();
      this.handleSafeAreaChanges();
    }

    this.storageService.storageReady$.subscribe(async () => {
      await Promise.all([
        this.authStore.setApiKey(),
        this.authStore.loadUserData(),
        this.friendsStore.loadMostlyUsedCurrencies()
      ])

      this.authStore.loadUserRegion().catch((res) => {
        console.error(res);
      });

      if(this.authStore.apiKey()) {
        await this.friendsStore.loadFriends()
      }
    });
  }

  private async setSafeAreaInsects() {
    try {
      const { insets } = await SafeArea.getSafeAreaInsets();
      document.documentElement.style.setProperty('--ion-safe-area-top', `${insets.top}px`);
      document.documentElement.style.setProperty('--ion-safe-area-right', `${insets.right}px`);
      document.documentElement.style.setProperty('--ion-safe-area-bottom', `${insets.bottom}px`);
      document.documentElement.style.setProperty('--ion-safe-area-left', `${insets.left}px`);
    } catch (e) {
      console.error('Error getting safe area insets:', e);
    }
  }

  private handleSafeAreaChanges() {
    SafeArea.addListener('safeAreaChanged', ({ insets }) => {
      document.documentElement.style.setProperty('--ion-safe-area-top', `${insets.top}px`);
      document.documentElement.style.setProperty('--ion-safe-area-bottom', `${insets.bottom}px`);
    });
  }
}
