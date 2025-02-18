import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import {
  IonHeader,
  IonToolbar,
  IonTitle,
  IonContent,
  IonButton,
  IonIcon,
   IonButtons,
} from '@ionic/angular/standalone';
import { AuthStore } from '../login/auth.store';

@Component({
  selector: 'app-activity-log',
  templateUrl: 'activity-log.page.html',
  styleUrls: ['activity-log.page.scss'],
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonIcon, IonButtons],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActivityLogPage {
  readonly authStore = inject(AuthStore);
  constructor() {}
}
