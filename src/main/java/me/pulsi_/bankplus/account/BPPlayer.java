package me.pulsi_.bankplus.account;

import me.pulsi_.bankplus.bankSystem.Bank;
import me.pulsi_.bankplus.utils.BPScheduler;
import org.bukkit.entity.Player;

public class BPPlayer {

    private final Player player;

    private Bank openedBank;
    private BPScheduler.TaskWrapper bankUpdatingTask, closingTask;

    // Values to check if the player is doing a deposit or withdraw through chat.
    private boolean depositing, withdrawing;

    public BPPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public Bank getOpenedBank() {
        return openedBank;
    }

    public BPScheduler.TaskWrapper getBankUpdatingTask() {
        return bankUpdatingTask;
    }

    public BPScheduler.TaskWrapper getClosingTask() {
        return closingTask;
    }

    public boolean isDepositing() {
        return depositing;
    }

    public boolean isWithdrawing() {
        return withdrawing;
    }

    public void setOpenedBank(Bank openedBank) {
        this.openedBank = openedBank;
    }

    public void setBankUpdatingTask(BPScheduler.TaskWrapper bankUpdatingTask) {
        this.bankUpdatingTask = bankUpdatingTask;
    }

    public void setClosingTask(BPScheduler.TaskWrapper closingTask) {
        this.closingTask = closingTask;
    }

    public void setDepositing(boolean depositing) {
        this.depositing = depositing;
    }

    public void setWithdrawing(boolean withdrawing) {
        this.withdrawing = withdrawing;
    }
}