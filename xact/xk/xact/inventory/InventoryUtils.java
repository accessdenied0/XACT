package xk.xact.inventory;


import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class InventoryUtils {

	/**
	 * Whether if two stacks contain the same kind of items.
	 *
	 * @param stack1     one of the stacks.
	 * @param stack2     one of the stacks.
	 * @param compareNBT if the item's NBT must also match.
	 * @return true if the stacks contain the same kind of items.
	 */
	public static boolean similarStacks(ItemStack stack1, ItemStack stack2, boolean compareNBT) {
		if( stack1 == null || stack2 == null )
			return false; // this scenario is not meant to happen. 

		if( stack1.itemID != stack2.itemID )
			return false;

		if( stack1.getHasSubtypes() ) {
			if( stack1.getItemDamage() != stack2.getItemDamage() )
				return false;
		}
		if( !compareNBT )
			return true;

		// Compare stacks tags.
		return !stack1.hasTagCompound() || stack2.hasTagCompound() && stack1.getTagCompound().equals( stack2.getTagCompound() );

	}

	/**
	 * Determines the available space in target to fit the contents of itemStack.
	 * In other words, the amount of itemStack that could be merged into target.
	 * <p/>
	 * The accuracy of this method relays on if both stacks hold the same kind of items.
	 * Note: the return value may be higher than itemStack.stackSize
	 *
	 * @param target    the stack where the contents of itemStack should fit.
	 * @param itemStack the stack that's meant to be merged.
	 * @return the amount of itemStack that would fit on target. Or -1, if the stacks is aren't of the same kind.
	 * @see InventoryUtils#similarStacks
	 */
	public static int getSpaceInStackFor(ItemStack target, ItemStack itemStack) {
		if( !similarStacks( target, itemStack, true ) )
			return -1;

		if( !target.isStackable() )
			return 0;

		int max = Math.min( target.getMaxStackSize(), itemStack.getMaxStackSize() );
		return max - target.stackSize;
	}

	/**
	 * Tries to add a stack on a inventory,
	 * and will return the remaining part that couldn't fit (if any).
	 * <p/>
	 * Note: the stack passed is manipulated (it's stack size is reduced).
	 *
	 * @param stack       the ItemStack that's meant to be added to the inventory.
	 * @param inv         the IInventory where the item should be added.
	 * @param ignoreEmpty if empty slots should be ignored.
	 * @return the remaining part of the passed stack. or null, if it was successfully added entirely.
	 */
	public static ItemStack addStackToInventory(ItemStack stack, IInventory inv, boolean ignoreEmpty) {
		if( stack == null )
			return null; // kinda like success... isn't it?

		/*
			Possible scenarios:
			1) the slot is empty (it might be ignored, if: ignoreEmpty == true)

			2) the slot has no space fot this items (probably because is not of the same kind).

			3) the slot has some available space for this item.
				a) there is enough space for it to fit.
				b) there is not enough space for it to fit entirely.
					So, split and find the next available slot.
			 */

		int remaining = stack.stackSize;
		for( InvSlot slot : InvSlotIterator.createNewFor( inv ) ) {
			if( slot == null )
				continue;

			if( slot.isEmpty() ) {
				// there are a few scenarios where you'd want to keep empty slots empty.
				if( ignoreEmpty ) continue;

				// the slot is empty, so feel free to add it.
				inv.setInventorySlotContents( slot.slotIndex, stack.copy() );
				inv.onInventoryChanged();
				stack.stackSize = 0;
				return null; // success
			}

			// how much of stack can fit on the slot?
			int space = slot.getSpaceFor( stack );
			if( space <= 0 )
				continue; // can't fit here.

			if( space >= stack.stackSize ) { // fits entirely
				slot.stack.stackSize += remaining;
				inv.onInventoryChanged();
				stack.stackSize = 0;
				return null; // success

			} else { // fits partially
				slot.stack.stackSize = slot.stack.getMaxStackSize();
				remaining -= space;
				// inv.onInventoryChanged();
			}
		}
		if( remaining == 0 )
			return null; // success

		stack.stackSize = remaining;
		return stack;
	}

	public static ItemStack[] getContents(IInventory inventory) {
		int size = inventory.getSizeInventory();
		ItemStack[] contents = new ItemStack[size];

		for( int i = 0; i < size; i++ )
			contents[i] = inventory.getStackInSlot( i );

		return contents;
	}

}