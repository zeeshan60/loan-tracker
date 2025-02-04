import { Component } from '@angular/core';
import { IonHeader, IonToolbar, IonTitle, IonContent } from '@ionic/angular/standalone';

@Component({
  selector: 'app-add-new',
  templateUrl: 'add-new.page.html',
  styleUrls: ['add-new.page.scss'],
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonContent]
})
export class AddNewPage {

  constructor() {}

}
