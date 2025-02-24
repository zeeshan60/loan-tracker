import { ChangeDetectionStrategy, Component, inject, OnInit, viewChild } from '@angular/core';
import { IonTabs, IonTabBar, IonTabButton, IonIcon, ModalController } from '@ionic/angular/standalone';
import { AddNewPage } from '../add-new/add-new.page';
import { Router } from '@angular/router';

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
  readonly router = inject(Router);
  constructor() {
  }

  async addNewExpense() {
    let selectedFriend = null;
    if (this.router.url.endsWith('tabs/friends')) {
      selectedFriend = { name: 'Zeeshan' };
    }
    const modal = await this.modalCtrl.create({
      component: AddNewPage,
      componentProps: { selectedFriend }
    })
    modal.present();
  }
}
