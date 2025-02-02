import { Component } from '@angular/core';
import { IonApp, IonRouterOutlet } from '@ionic/angular/standalone';
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
  ],
})
export class AppComponent {
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
  }
}
