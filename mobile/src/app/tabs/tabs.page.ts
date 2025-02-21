import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { IonTabs, IonTabBar, IonTabButton, IonIcon, ModalController } from '@ionic/angular/standalone';
import { AddNewPage } from '../add-new/add-new.page';

@Component({
  selector: 'app-tabs',
  templateUrl: 'tabs.page.html',
  styleUrls: ['tabs.page.scss'],
  standalone: true,
  imports: [IonTabs, IonTabBar, IonTabButton, IonIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TabsPage {
  readonly modalCtrl = inject(ModalController);
  constructor() {
  }

  async addNewExpense() {
    console.log('add new expense...');
    const modal = await this.modalCtrl.create({
      component: AddNewPage,
      componentProps: {
        selectedFriend: { name: 'Zeeshan'}
      }
    })
    modal.present();
  }
}
