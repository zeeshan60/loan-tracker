import { ChangeDetectionStrategy, Component } from '@angular/core';
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

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html',
  standalone: true,
  imports: [
    IonApp,
    IonRouterOutlet,
    IonLoading,
    IonButton,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent {
  constructor(private loadingController: LoadingController) {
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
    this.showLoading();
  }

  async showLoading() {
    const loading = await this.loadingController.create({
      message: 'Dismissing after 3 seconds...',
    });

    loading.present();
    setTimeout(() => {
      loading.dismiss();
    }, 4000)
    // loading.dismiss();
  }
}
