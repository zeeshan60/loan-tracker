import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { IonApp, IonButton, IonLoading, IonRouterOutlet, LoadingController } from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  ellipse,
  logoGoogle,
  square,
  triangle,
  peopleOutline,
  settingsOutline,
  addCircleOutline,
  logOutOutline
} from 'ionicons/icons';
import { signalMethod } from '@ngrx/signals';
import { FriendsStore } from './store/friends.store';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html',
  standalone: true,
  imports: [
    IonApp,
    IonRouterOutlet,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent {
  readonly loadingCtrl = inject(LoadingController);

  private loader: HTMLIonLoadingElement | null = null;

  readonly activateLoaderWhen = signalMethod<boolean>((isLoading) => {
    if (!this.loader) {
      return;
    }
    if (isLoading) {
      this.loader.present();
    } else {
      this.loader.dismiss();
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
      logOutOutline
    });

    this.loadDependencies().then(() => {
      this.activateLoaderWhen(this.friendsStore.loading);
    });
  }

  async loadDependencies() {
    this.loader = await this.loadingCtrl.create();
  }
}
