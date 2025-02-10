import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
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

  readonly activateLoaderWhen = signalMethod<boolean>(async (isLoading) => {
    console.log(isLoading);
    if (!this.loader && isLoading) {
      this.loader = await this.loadingCtrl.create();
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
}
