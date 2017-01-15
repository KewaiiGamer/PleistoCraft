package com.elseytd.pleistocraft.entitys;

import com.elseytd.pleistocraft.registries.ItemsRegistry;
import com.elseytd.pleistocraft.utils.Tools;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import java.util.UUID;
import javax.annotation.Nullable;

import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiScreenHorseInventory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.IJumpingMount;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.AnimalChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.IInventoryChangedListener;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.server.management.PreYggdrasilConverter;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.datafix.walkers.ItemStackData;
import net.minecraft.util.datafix.walkers.ItemStackDataLists;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EntitySmilodonPopulator extends EntityTameable implements IInventoryChangedListener, IJumpingMount
{
    private static final Predicate<Entity> IS_SMILODON_BREEDING = new Predicate<Entity>()
    {
        public boolean apply(@Nullable Entity p_apply_1_)
        {
            return p_apply_1_ instanceof EntitySmilodonPopulator && ((EntitySmilodonPopulator)p_apply_1_).isBreeding();
        }
    };
    private static final IAttribute JUMP_STRENGTH = (new RangedAttribute((IAttribute)null, "horse.jumpStrength", 0.7D, 0.0D, 2.0D)).setDescription("Jump Strength").setShouldWatch(true);
    //private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("556E1665-8B10-40C8-8F9D-CF9B1667F295");
    private static final DataParameter<Byte> STATUS = EntityDataManager.<Byte>createKey(EntitySmilodonPopulator.class, DataSerializers.BYTE);
    //private static final DataParameter<Integer> HORSE_TYPE = EntityDataManager.<Integer>createKey(EntitySmilodonPopulator.class, DataSerializers.VARINT);
    //private static final DataParameter<Integer> HORSE_VARIANT = EntityDataManager.<Integer>createKey(EntitySmilodonPopulator.class, DataSerializers.VARINT);
    private static final DataParameter<Optional<UUID>> OWNER_UNIQUE_ID = EntityDataManager.<Optional<UUID>>createKey(EntitySmilodonPopulator.class, DataSerializers.OPTIONAL_UNIQUE_ID);
    //private static final DataParameter<Integer> HORSE_ARMOR = EntityDataManager.<Integer>createKey(EntitySmilodonPopulator.class, DataSerializers.VARINT);
    private static final String[] HORSE_TEXTURES = new String[] {"textures/entity/horse/horse_white.png", "textures/entity/horse/horse_creamy.png", "textures/entity/horse/horse_chestnut.png", "textures/entity/horse/horse_brown.png", "textures/entity/horse/horse_black.png", "textures/entity/horse/horse_gray.png", "textures/entity/horse/horse_darkbrown.png"};
    private static final String[] HORSE_TEXTURES_ABBR = new String[] {"hwh", "hcr", "hch", "hbr", "hbl", "hgr", "hdb"};
    private static final String[] HORSE_MARKING_TEXTURES = new String[] {null, "textures/entity/horse/horse_markings_white.png", "textures/entity/horse/horse_markings_whitefield.png", "textures/entity/horse/horse_markings_whitedots.png", "textures/entity/horse/horse_markings_blackdots.png"};
    private static final String[] HORSE_MARKING_TEXTURES_ABBR = new String[] {"", "wo_", "wmo", "wdo", "bdo"};
    //private final EntityAISkeletonRiders skeletonTrapAI = new EntityAISkeletonRiders(this);
    private int mode = 0;//0 - move freely, 1 - follow, 2-stay
    private Minecraft mc;
    private int eatingHaystackCounter;
    private int openMouthCounter;
    private int jumpRearingCounter;
    public int tailCounter;
    public int sprintCounter;
    protected boolean smilodonJumping;
    private AnimalChest smilodonChest;
    private boolean hasReproduced;
    /** "The higher this value, the more likely the smilodon is to be tamed next time a player rides it." */
    protected int temper;
    protected float jumpPower;
    private boolean allowStandSliding;
    //private boolean skeletonTrap;
    //private int skeletonTrapTime;
    private float headLean;
    private float prevHeadLean;
    private float rearingAmount;
    private float prevRearingAmount;
    private float mouthOpenness;
    private float prevMouthOpenness;
    /** Used to determine the sound that the smilodon should make when it steps */
    private int gallopTime;
    private String texturePrefix;
    private final String[] smilodonTexturesArray = new String[3];
    @SideOnly(Side.CLIENT)
    private boolean hasTexture;
    private SoundEvent ambientSound;
    private SoundEvent deathSound;
    private SoundEvent hurtSound;

    public EntitySmilodonPopulator(World worldIn)
    {
        super(worldIn);
        //this.setSize(1.3964844F, 1.6F);
        if (this.isChild()) {
            this.setSize(1.5F, 0.8F);
        } else {
            this.setSize(0.8F, 1.6F);
        }
        this.isImmuneToFire = false;
        this.setChested(false);
        this.stepHeight = 1.0F;
        this.initSmilodonChest();
    }

    @Override
    protected void initEntityAI() {
        int i = 0;
//      ((PathNavigateGround) this.getNavigator()).setAvoidsWater(true);
        this.tasks.addTask(i++, new EntityAISwimming(this));
        //this.tasks.addTask(i++, this.aiSit);
        this.tasks.addTask(i++, new EntityAIFollowOwner(this, 1.0D, 10.0F, 2.0F));
        this.tasks.addTask(i++, new EntityAILeapAtTarget(this, 0.4F));
        //      this.tasks.addTask(i++, new EntityAIAttackOnCollide(this, 1.0D, true));
        this.tasks.addTask(i++, new EntityAIMate(this, 1.0D));
        this.tasks.addTask(i++, new EntityAIWander(this, 1.0D));
        this.tasks.addTask(i++, new EntityAIWatchClosest(this, EntityPlayer.class, 12.0F));
        this.tasks.addTask(i++, new EntityAILookIdle(this));
        i = 0;
        this.targetTasks.addTask(i++, new EntityAIOwnerHurtByTarget(this));
        this.targetTasks.addTask(i++, new EntityAIOwnerHurtTarget(this));
        this.targetTasks.addTask(i++, new EntityAIHurtByTarget(this, true, new Class[0]));

        this.setTamed(false);
    }
    protected void entityInit()
    {
        super.entityInit();
        this.dataManager.register(STATUS, Byte.valueOf((byte)0));
        //this.dataManager.register(HORSE_TYPE, Integer.valueOf(SmilodonType.HORSE.getOrdinal()));
        //this.dataManager.register(HORSE_VARIANT, Integer.valueOf(0));
        this.dataManager.register(OWNER_UNIQUE_ID, Optional.<UUID>absent());
        //this.dataManager.register(HORSE_ARMOR, Integer.valueOf(SmilodonArmorType.NONE.getOrdinal()));
    }

    /**
     * Copied from old entity
     */


    @Override
    protected void dropFewItems(boolean p_70628_1_, int p_70628_2_) {
        int i = Tools.randint(0, 2);

        for (int k = 0; k < i; ++k) {
            this.dropItem(Items.BONE, 1);
        }

        i = Tools.randint(1, 3);

        for (int k = 0; k < i; ++k) {
            if (this.isBurning()) {
                this.dropItem(ItemsRegistry.cooked_smilodon_meat, 1);
            } else {
                this.dropItem(ItemsRegistry.raw_smilodon_meat, 1);
            }
        }

        i = Tools.randint(0, 1);

        for (int k = 0; k < i; ++k) {
            this.dropItem(ItemsRegistry.smilodon_populator_skull, 1);
        }
    }
    /*
    public void setType(SmilodonType armorType)

    {
        this.dataManager.set(HORSE_TYPE, Integer.valueOf(armorType.getOrdinal()));
        this.resetTexturePrefix();
    }

    public SmilodonType getType()
    {
        return SmilodonType.getArmorType(((Integer)this.dataManager.get(HORSE_TYPE)).intValue());
    }

    public void setSmilodonVariant(int variant)
    {
        this.dataManager.set(HORSE_VARIANT, Integer.valueOf(variant));
        this.resetTexturePrefix();
    }

    public int ro()
    {
        return ((Integer)this.dataManager.get(HORSE_VARIANT)).intValue();
    }
    */

    /**
     * Get the name of this object. For players this returns their username
     */
    public String getName()
    {
        //return this.hasCustomName() ? this.getCustomNameTag() : this.getName();
        return "Smilodon";
    }

    private boolean getSmilodonWatchableBoolean(int p_110233_1_)
    {
        return (((Byte)this.dataManager.get(STATUS)).byteValue() & p_110233_1_) != 0;
    }

    private void setSmilodonWatchableBoolean(int p_110208_1_, boolean p_110208_2_)
    {
        byte b0 = ((Byte)this.dataManager.get(STATUS)).byteValue();

        if (p_110208_2_)
        {
            this.dataManager.set(STATUS, Byte.valueOf((byte)(b0 | p_110208_1_)));
        }
        else
        {
            this.dataManager.set(STATUS, Byte.valueOf((byte)(b0 & ~p_110208_1_)));
        }
    }

    public boolean isAdultSmilodon()
    {
        return !this.isChild();
    }

    public boolean isTame()
    {
        return this.getSmilodonWatchableBoolean(2);
    }

    public boolean isRidable()
    {
        return this.isAdultSmilodon();
    }

    @Nullable
    public UUID getOwnerUniqueId()
    {
        return (UUID)((Optional)this.dataManager.get(OWNER_UNIQUE_ID)).orNull();
    }

    public void setOwnerUniqueId(@Nullable UUID uniqueId)
    {
        this.dataManager.set(OWNER_UNIQUE_ID, Optional.fromNullable(uniqueId));
    }

    public float getSmilodonSize()
    {
        return 0.5F;
    }

    /**
     * "Sets the scale for an ageable entity according to the boolean parameter, which says if it's a child."
     */
    public void setScaleForAge(boolean child)
    {
        if (child)
        {
            this.setScale(this.getSmilodonSize());
        }
        else
        {
            this.setScale(1.0F);
        }
    }

    public boolean isSmilodonJumping()
    {
        return this.smilodonJumping;
    }

    public void setSmilodonTamed(boolean tamed)
    {
        this.setSmilodonWatchableBoolean(2, tamed);
    }

    public void setSmilodonJumping(boolean jumping)
    {
        this.smilodonJumping = jumping;
    }

    public boolean canBeLeashedTo(EntityPlayer player)
    {
        return super.canBeLeashedTo(player);
    }

    protected void onLeashDistance(float p_142017_1_)
    {
        if (p_142017_1_ > 6.0F && this.isEatingHaystack())
        {
            this.setEatingHaystack(false);
        }
    }

    public boolean isChested()
    {
        return this.getSmilodonWatchableBoolean(8);
    }

    /*
    public SmilodonArmorType getSmilodonArmorType()

    {
        return SmilodonArmorType.getByOrdinal(((Integer)this.dataManager.get(HORSE_ARMOR)).intValue());
    }
    */


    public boolean isEatingHaystack()
    {
        return this.getSmilodonWatchableBoolean(32);
    }

    public boolean isRearing()
    {
        return this.getSmilodonWatchableBoolean(64);
    }

    public boolean isBreeding()
    {
        return this.getSmilodonWatchableBoolean(16);
    }

    public boolean getHasReproduced()
    {
        return this.hasReproduced;
    }

    /**
     * Set smilodon armor stack (for example: new ItemStack(Items.iron_smilodon_armor))
     */
   /* public void setSmilodonArmorStack(ItemStack itemStackIn)
    {
        SmilodonArmorType smilodonarmortype = SmilodonArmorType.getByItemStack(itemStackIn);
        this.dataManager.set(HORSE_ARMOR, Integer.valueOf(smilodonarmortype.getOrdinal()));
        this.resetTexturePrefix();

        if (!this.worldObj.isRemote)
        {
            this.getEntityAttribute(SharedMonsterAttributes.ARMOR).removeModifier(ARMOR_MODIFIER_UUID);
            int i = smilodonarmortype.getProtection();

            if (i != 0)
            {
                this.getEntityAttribute(SharedMonsterAttributes.ARMOR).applyModifier((new AttributeModifier(ARMOR_MODIFIER_UUID, "Smilodon armor bonus", (double)i, 0)).setSaved(false));
            }
        }
    }
    */

    public void setBreeding(boolean breeding)
    {
        this.setSmilodonWatchableBoolean(16, breeding);
    }

    public void setChested(boolean chested)
    {
        this.setSmilodonWatchableBoolean(8, chested);
    }

    public void setHasReproduced(boolean hasReproducedIn)
    {
        this.hasReproduced = hasReproducedIn;
    }

    public void setSmilodonSaddled(boolean saddled)
    {
        this.setSmilodonWatchableBoolean(4, saddled);
    }

    public int getTemper()
    {
        return this.temper;
    }

    public void setTemper(int temperIn)
    {
        this.temper = temperIn;
    }

    public int increaseTemper(int p_110198_1_)
    {
        int i = MathHelper.clamp_int(this.getTemper() + p_110198_1_, 0, this.getMaxTemper());
        this.setTemper(i);
        return i;
    }

    /**
     * Called when the entity is attacked.
     */
    public boolean attackEntityFrom(DamageSource source, float amount)
    {
        Entity entity = source.getEntity();
        return this.isBeingRidden() && entity != null && this.isRidingOrBeingRiddenBy(entity) ? false : super.attackEntityFrom(source, amount);
    }

    /**
     * Returns true if this entity should push and be pushed by other entities when colliding.
     */
    public boolean canBePushed()
    {
        return !this.isBeingRidden();
    }

    public boolean prepareChunkForSpawn()
    {
        int i = MathHelper.floor_double(this.posX);
        int j = MathHelper.floor_double(this.posZ);
        this.worldObj.getBiome(new BlockPos(i, 0, j));
        return true;
    }

    public void dropChests()
    {
        if (!this.worldObj.isRemote && this.isChested())
        {
            this.dropItem(Item.getItemFromBlock(Blocks.CHEST), 1);
            this.setChested(false);
        }
    }

    private void eatingSmilodon()
    {
        this.openSmilodonMouth();

        if (!this.isSilent())
        {
            this.worldObj.playSound((EntityPlayer)null, this.posX, this.posY, this.posZ, SoundEvents.ENTITY_HORSE_EAT, this.getSoundCategory(), 1.0F, 1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F);
        }
    }

    public void fall(float distance, float damageMultiplier)
    {
        if (distance > 1.0F)
        {
            this.playSound(SoundEvents.ENTITY_HORSE_LAND, 0.4F, 1.0F);
        }

        int i = MathHelper.ceiling_float_int((distance * 0.5F - 3.0F) * damageMultiplier);

        if (i > 0)
        {
            this.attackEntityFrom(DamageSource.fall, (float)i);

            if (this.isBeingRidden())
            {
                for (Entity entity : this.getRecursivePassengers())
                {
                    entity.attackEntityFrom(DamageSource.fall, (float)i);
                }
            }

            IBlockState iblockstate = this.worldObj.getBlockState(new BlockPos(this.posX, this.posY - 0.2D - (double)this.prevRotationYaw, this.posZ));
            Block block = iblockstate.getBlock();

            if (iblockstate.getMaterial() != Material.AIR && !this.isSilent())
            {
                SoundType soundtype = block.getSoundType(iblockstate, worldObj, new BlockPos(this.posX, this.posY - 0.2D - (double)this.prevRotationYaw, this.posZ), this);
                this.worldObj.playSound((EntityPlayer)null, this.posX, this.posY, this.posZ, soundtype.getStepSound(), this.getSoundCategory(), soundtype.getVolume() * 0.5F, soundtype.getPitch() * 0.75F);
            }
        }
    }

    /**
     * Returns number of slots depending smilodon type
     */
    private int getChestSize()
    {
        //SmilodonType smilodontype = this.getType();
        return this.isChested() ? 17 : 2;
    }

    private void initSmilodonChest()
    {
        AnimalChest animalchest = this.smilodonChest;
        this.smilodonChest = new AnimalChest("SmilodonChest", this.getChestSize());
        this.smilodonChest.setCustomName(this.getName());

        if (animalchest != null)
        {
            animalchest.removeInventoryChangeListener(this);
            int i = Math.min(animalchest.getSizeInventory(), this.smilodonChest.getSizeInventory());

            for (int j = 0; j < i; ++j)
            {
                ItemStack itemstack = animalchest.getStackInSlot(j);

                if (itemstack != null)
                {
                    this.smilodonChest.setInventorySlotContents(j, itemstack.copy());
                }
            }
        }

        this.smilodonChest.addInventoryChangeListener(this);
        this.updateSmilodonSlots();
        this.itemHandler = new net.minecraftforge.items.wrapper.InvWrapper(this.smilodonChest);
    }

    /**
     * Updates the items in the saddle and armor slots of the smilodon's inventory.
     */
    private void updateSmilodonSlots()
    {
        if (!this.worldObj.isRemote)
        {
            this.setSmilodonSaddled(this.smilodonChest.getStackInSlot(0) != null);

            /*if (this.isSmilodon())
            {
                this.setSmilodonArmorStack(this.smilodonChest.getStackInSlot(1));
            }
            */
        }
    }

    /**
     * Called by InventoryBasic.onInventoryChanged() on a array that is never filled.
     */
    public void onInventoryChanged(InventoryBasic invBasic)
    {
        //SmilodonArmorType smilodonarmortype = this.getSmilodonArmorType();
        boolean flag = this.isSmilodonSaddled();
        this.updateSmilodonSlots();

        if (this.ticksExisted > 20)
        {
            /*if (smilodonarmortype == SmilodonArmorType.NONE && smilodonarmortype != this.getSmilodonArmorType())
            {
                this.playSound(SoundEvents.ENTITY_HORSE_ARMOR, 0.5F, 1.0F);
            }
            else if (smilodonarmortype != this.getSmilodonArmorType())
            {
                this.playSound(SoundEvents.ENTITY_HORSE_ARMOR, 0.5F, 1.0F);
            }
            */

            if (!flag && this.isSmilodonSaddled())
            {
                this.playSound(SoundEvents.ENTITY_HORSE_SADDLE, 0.5F, 1.0F);
            }
        }
    }

    /**
     * Checks if the entity's current position is a valid location to spawn this entity.
     */
    public boolean getCanSpawnHere()
    {
        this.prepareChunkForSpawn();
        return super.getCanSpawnHere();
    }

    protected EntitySmilodonPopulator getClosestSmilodon(Entity entityIn, double distance)
    {
        double d0 = Double.MAX_VALUE;
        Entity entity = null;

        for (Entity entity1 : this.worldObj.getEntitiesInAABBexcluding(entityIn, entityIn.getEntityBoundingBox().addCoord(distance, distance, distance), IS_SMILODON_BREEDING))
        {
            double d1 = entity1.getDistanceSq(entityIn.posX, entityIn.posY, entityIn.posZ);

            if (d1 < d0)
            {
                entity = entity1;
                d0 = d1;
            }
        }

        return (EntitySmilodonPopulator)entity;
    }

    public double getSmilodonJumpStrength()
    {
        return this.getEntityAttribute(JUMP_STRENGTH).getAttributeValue();
    }

    protected SoundEvent getDeathSound()
    {
        this.openSmilodonMouth();
        return this.deathSound;
    }

    protected SoundEvent getHurtSound()
    {
        this.openSmilodonMouth();

        if (this.rand.nextInt(3) == 0)
        {
            this.makeSmilodonRear();
        }

        return this.hurtSound;
    }

    public boolean isSmilodonSaddled()
    {
        return this.getSmilodonWatchableBoolean(4);
    }

    protected SoundEvent getAmbientSound()
    {
        this.openSmilodonMouth();

        if (this.rand.nextInt(10) == 0 && !this.isMovementBlocked())
        {
            this.makeSmilodonRear();
        }

        return this.ambientSound;
    }



    @Nullable
    protected SoundEvent getAngrySound()
    {
        this.openSmilodonMouth();
        this.makeSmilodonRear();
        //SmilodonType smilodontype = this.getType();
        //return smilodontype.isUndead() ? null : (smilodontype.hasMuleEars() ? SoundEvents.ENTITY_DONKEY_ANGRY : SoundEvents.ENTITY_HORSE_ANGRY);
        return SoundEvents.ENTITY_WOLF_HOWL;
    }


    /**
     * Determines whether this wolf is angry or not.
     *
     * @return
     */
    public boolean isAngry() {
        //return (this.dataWatcher.getWatchableObjectByte(16) & 2) != 0;
        return (((Byte)this.dataManager.get(TAMED)).byteValue() & 2) != 0;
    }

    /**
     * Sets whether this wolf is angry or not.
     *
     * @param angry
     */
    public void setAngry(boolean angry) {
        //byte b0 = this.dataManager.getWatchableObjectByte(16);
        byte b0 = this.dataManager.get(TAMED);

        if (angry) {
            //this.dataWatcher.updateObject(16, (byte) (b0 | 2));
            this.dataManager.set(TAMED, Byte.valueOf((byte)(b0 | 2)));
        } else {
            this.dataManager.set(TAMED, Byte.valueOf((byte)(b0 & -3)));
            //this.dataWatcher.updateObject(16, (byte) (b0 & -3));
        }
    }
    protected void playStepSound(BlockPos pos, Block blockIn)
    {
        SoundType soundtype = blockIn.getSoundType(worldObj.getBlockState(pos), worldObj, pos, this);

        if (this.worldObj.getBlockState(pos.up()).getBlock() == Blocks.SNOW_LAYER)
        {
            soundtype = Blocks.SNOW_LAYER.getSoundType();
        }

        if (!blockIn.getDefaultState().getMaterial().isLiquid())
        {
            if (this.isBeingRidden())
            {
                ++this.gallopTime;

                if (this.gallopTime > 5 && this.gallopTime % 3 == 0)
                {
                    this.playSound(SoundEvents.ENTITY_HORSE_GALLOP, soundtype.getVolume() * 0.15F, soundtype.getPitch());

                    if (this.rand.nextInt(10) == 0)
                    {
                        this.playSound(SoundEvents.ENTITY_HORSE_BREATHE, soundtype.getVolume() * 0.6F, soundtype.getPitch());
                    }
                }
                else if (this.gallopTime <= 5)
                {
                    this.playSound(SoundEvents.ENTITY_HORSE_STEP_WOOD, soundtype.getVolume() * 0.15F, soundtype.getPitch());
                }
            }
            else if (soundtype == SoundType.WOOD)
            {
                this.playSound(SoundEvents.ENTITY_HORSE_STEP_WOOD, soundtype.getVolume() * 0.15F, soundtype.getPitch());
            }
            else
            {
                this.playSound(SoundEvents.ENTITY_HORSE_STEP, soundtype.getVolume() * 0.15F, soundtype.getPitch());
            }
        }
    }
/*
    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getAttributeMap().registerAttribute(JUMP_STRENGTH);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(40.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.5D);
    }
*/

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(40.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(2D);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(8.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(40.0D);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(5.0D);
    }
    /**
     * Will return how many at most can spawn in a chunk at once.
     */
    public int getMaxSpawnedInChunk()
    {
        return 6;
    }

    public int getMaxTemper()
    {
        return 100;
    }

    /**
     * Returns the volume for the sounds this mob makes.
     */
    protected float getSoundVolume()
    {
        return 0.8F;
    }

    /**
     * Get number of ticks, at least during which the living entity will be silent.
     */
    public int getTalkInterval()
    {
        return 400;
    }

    /*@SideOnly(Side.CLIENT)
    public boolean hasLayeredTextures()
    {
        return this.getType() == SmilodonType.HORSE || this.getSmilodonArmorType() != SmilodonArmorType.NONE;
    }
    */

    private void resetTexturePrefix()
    {
        this.texturePrefix = null;
    }

    /*
    @SideOnly(Side.CLIENT)
    /*
    public boolean hasTexture()
    {
        return this.hasTexture;
    }
    */

  /*  @SideOnly(Side.CLIENT)
    private void setSmilodonTexturePaths()
    {
        this.texturePrefix = "smilodon/";
        this.smilodonTexturesArray[0] = null;
        this.smilodonTexturesArray[1] = null;
        this.smilodonTexturesArray[2] = null;
        SmilodonType smilodontype = this.getType();
        int i = this.getSmilodonVariant();

        if (smilodontype == SmilodonType.HORSE)
        {
            int j = i & 255;
            int k = (i & 65280) >> 8;

            if (j >= HORSE_TEXTURES.length)
            {
                this.hasTexture = false;
                return;
            }

            this.smilodonTexturesArray[0] = HORSE_TEXTURES[j];
            this.texturePrefix = this.texturePrefix + HORSE_TEXTURES_ABBR[j];

            if (k >= HORSE_MARKING_TEXTURES.length)
            {
                this.hasTexture = false;
                return;
            }

            this.smilodonTexturesArray[1] = HORSE_MARKING_TEXTURES[k];
            this.texturePrefix = this.texturePrefix + HORSE_MARKING_TEXTURES_ABBR[k];
        }
        else
        {
            this.smilodonTexturesArray[0] = "";
            this.texturePrefix = this.texturePrefix + "_" + smilodontype + "_";
        }

        SmilodonArmorType smilodonarmortype = this.getSmilodonArmorType();
        this.smilodonTexturesArray[2] = smilodonarmortype.getTextureName();
        this.texturePrefix = this.texturePrefix + smilodonarmortype.getHash();
        this.hasTexture = true;
    }

*/
  /*
    @SideOnly(Side.CLIENT)
    public String getSmilodonTexture()
    {
        if (this.texturePrefix == null)
        {
            this.setSmilodonTexturePaths();
        }

        return this.texturePrefix;
    }

    @SideOnly(Side.CLIENT)
    public String[] getVariantTexturePaths()
    {
        if (this.texturePrefix == null)
        {
            this.setSmilodonTexturePaths();
        }

        return this.smilodonTexturesArray;
    }
*/
    public void openGUI(EntityPlayer playerEntity)
    {
        if (!this.worldObj.isRemote && (!this.isBeingRidden() || this.isPassenger(playerEntity)) && this.isTame())
        {
            this.smilodonChest.setCustomName(this.getName());
            //this.openGuiSmilodonInventory(this, this.smilodonChest);
        }
    }

/*    public static void openGuiSmilodonInventory(EntitySmilodonPopulator smilodonPopulator, IInventory inventoryIn) {

        this.mc.displayGuiScreen(new GuiScreenSmilodonInventory(EntityPlayerSP.inventory, inventoryIn, smilodonPopulator));
    }/*
    /*public boolean processInteract(EntityPlayer player, EnumHand hand, @Nullable ItemStack stack)
    {
        if (stack != null && stack.getItem() == Items.SPAWN_EGG)
        {
            return super.processInteract(player, hand, stack);
        }
        /*
        else if (!this.isTame())
        {
            return false;
        }
        */
/*
        else if (this.isTame() && this.isAdultSmilodon() && player.isSneaking())
        {
            this.openGUI(player);
            return true;
        }
        else if (this.isRidable() && this.isBeingRidden())
        {
            return super.processInteract(player, hand, stack);
        }
        else
        {
            if (stack != null)
            {

                //SmilodonArmorType smilodonarmortype = SmilodonArmorType.getByItemStack(stack);

                //if (smilodonarmortype != SmilodonArmorType.NONE)
                //{
                if (!this.isTame())
                {
                    this.makeSmilodonRearWithSound();
                    return true;
                }

                this.openGUI(player);
                return true;
            }

                boolean flag = false;

                float f = 0.0F;
                int i = 0;
                int j = 0;

                if (stack.getItem() == Items.BEEF) {
                    f = 3.0F;
                    i = 20;
                    j = 3;
                    if (this.isTame() && this.getGrowingAge() == 0 && !this.isInLove()) {
                        flag = true;
                        this.setInLove(player);
                    }
                }
                */
                /*
                else if (stack.getItem() == Items.SUGAR)
                {
                    f = 1.0F;
                    i = 30;
                    j = 3;
                }
                else if (Block.getBlockFromItem(stack.getItem()) == Blocks.HAY_BLOCK)
                {
                    f = 20.0F;
                    i = 180;
                }
                else if (stack.getItem() == Items.APPLE)
                {
                    f = 3.0F;
                    i = 60;
                    j = 3;
                }
                else if (stack.getItem() == Items.GOLDEN_CARROT)
                {
                    f = 4.0F;
                    i = 60;
                    j = 5;

                    if (this.isTame() && this.getGrowingAge() == 0)
                    {
                        flag = true;
                        this.setInLove(player);
                    }
                }
                else if (stack.getItem() == Items.GOLDEN_APPLE)
                {
                    f = 10.0F;
                    i = 240;
                    j = 10;
                    if (this.isTame() && this.getGrowingAge() == 0 && !this.isInLove())
                    {
                        flag = true;
                        this.setInLove(player);
                    }
                }
                */
                /*

                if (this.getHealth() < this.getMaxHealth() && f > 0.0F)
                {
                    this.heal(f);
                    flag = true;
                }
                if (!this.isAdultSmilodon() && i > 0)
                {
                    if (!this.worldObj.isRemote)
                    {
                        this.addGrowth(i);
                    }
                    flag = true;
                }

                if (j > 0 && (flag || !this.isTame()) && this.getTemper() < this.getMaxTemper())
                {
                    flag = true;

                    if (!this.worldObj.isRemote)
                    {
                        this.increaseTemper(j);
                    }
                }

                if (flag)
                {
                    this.eatingSmilodon();
                }
                if (!this.isTame() && !flag)
                {
                    if (stack.interactWithEntity(player, this, hand))
                    {
                        return true;
                    }

                    this.makeSmilodonRearWithSound();
                    return true;
                }

                if (!flag && !this.isChested() && stack.getItem() == Item.getItemFromBlock(Blocks.CHEST))
                {
                    this.setChested(true);
                    this.playSound(SoundEvents.ENTITY_DONKEY_CHEST, 1.0F, (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F);
                    flag = true;
                    this.initSmilodonChest();
                }

                if (!flag && this.isRidable() && !this.isSmilodonSaddled() && stack.getItem() == Items.SADDLE)
                {
                    this.openGUI(player);
                    return true;
                }

                if (flag)
                {
                    if (!player.capabilities.isCreativeMode)
                    {
                        --stack.stackSize;
                    }

                    return true;
                }
            }

            if (this.isRidable() && !this.isBeingRidden())
            {
                if (stack != null && stack.interactWithEntity(player, this, hand))
                {
                    return true;
                }
                else
                {
                    this.mountTo(player);
                    return true;
                }
            }
            else
            {
                return super.processInteract(player, hand, stack);
            }
        }
*/
    /**
     * old Smilodon Method
     *
     */


    public boolean processInteract(EntityPlayer player, EnumHand hand, @Nullable ItemStack itemStack) {
        ItemStack itemstack = player.inventory.getCurrentItem();
        if (!player.isSneaking()) {
            if (this.isTamed()) {
                if (itemstack != null) {
                    if (itemstack.getItem() instanceof ItemFood) {
                        ItemFood itemfood = (ItemFood) itemstack.getItem();

                        if (itemfood.isWolfsFavoriteMeat() &&
                                //this.dataWatcherList.getWatchableObjectFloat(18) < 30.0F) {
                                this.getHealth() < 30.0F) {
                            if (itemstack != new ItemStack(Items.ROTTEN_FLESH) && itemstack != new ItemStack(ItemsRegistry.raw_smilodon_meat) && itemstack != new ItemStack(ItemsRegistry.cooked_smilodon_meat)) {
                                if (!player.capabilities.isCreativeMode) {
                                    --itemstack.stackSize;
                                }

                                this.heal(3);

                                if (itemstack.stackSize <= 0) {
                                    player.inventory.setInventorySlotContents(player.inventory.currentItem, (ItemStack) null);
                                }
                            }
                            return true;
                        }
                    } else if (itemstack.getItem() == Items.SADDLE) {
                        if (!this.worldObj.isRemote && !this.isSmilodonSaddled()) {
                            this.setSmilodonJumping(true);
                        }
                    }
                } else if (this.worldObj.isRemote || !(this.isRidingOrBeingRiddenBy(player)) && !(this.isRidingOrBeingRiddenBy(null))) {

                } else {
                    if (!this.isChild() && this.isSmilodonSaddled()) {
                        this.mountTo(player);
                        return true;
                    }
                }

                if (this.isOwner(player) && !this.worldObj.isRemote && !this.isBreedingItem(itemstack)) {
                    //this.aiSit.setSitting(!this.isSitting());
                    this.isJumping = false;
                    this.navigator.clearPathEntity();
                    this.setAttackTarget((EntityLivingBase) null);
                }
            } else if (itemstack != null && itemstack.getItem() == Items.BEEF && !this.isAngry()) {
                if (!player.capabilities.isCreativeMode) {
                    --itemstack.stackSize;
                }

                if (itemstack.stackSize <= 0) {
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, (ItemStack) null);
                }

                if (!this.worldObj.isRemote) {
                    if (this.rand.nextInt(4) == 0) {
                        this.setTamed(true);
                        this.navigator.clearPathEntity();
                        this.setAttackTarget((EntityLivingBase) null);
                        this.setHealth(40.0F);
                        this.setOwnerId(player.getUniqueID());
                        this.playTameEffect(true);
                        this.worldObj.setEntityState(this, (byte) 7);
                    } else {
                        this.playTameEffect(false);
                        this.worldObj.setEntityState(this, (byte) 6);
                    }
                }

                return true;
            }
        } else {
            if (!this.worldObj.isRemote && this.isTamed()) {
                switch (mode) {
                    case 0:
                        player.addChatMessage(new TextComponentString(ChatFormatting.GOLD + "Set to 'follow' mode."));
                        //this.aiSit.setSitting(false);
                        mode = 1;
                        break;
                    case 1:
                        player.addChatMessage(new TextComponentString(ChatFormatting.GOLD + "Set to 'stay' mode."));
                        //this.aiSit.setSitting(true);
                        mode = 2;
                        break;
                    case 2:
                        player.addChatMessage(new TextComponentString(ChatFormatting.GOLD + "Set to 'move freely' mode."));
                        //this.aiSit.setSitting(false);
                        mode = 0;
                        break;
                }
            }
        }
        return super.processInteract(player, hand, itemStack);
    }
    private void mountTo(EntityPlayer player)
    {
        player.rotationYaw = this.rotationYaw;
        player.rotationPitch = this.rotationPitch;
        this.setEatingHaystack(false);
        this.setRearing(false);

        if (!this.worldObj.isRemote)
        {
            player.startRiding(this);
        }
    }

    /**
     * Dead and sleeping entities cannot move
     */
    protected boolean isMovementBlocked()
    {
        return this.isBeingRidden() && this.isSmilodonSaddled() ? true : this.isEatingHaystack() || this.isRearing();
    }

    /**
     * Checks if the parameter is an item which this animal can be fed to breed it (wheat, carrots or seeds depending on
     * the animal type)
     */
    public boolean isBreedingItem(@Nullable ItemStack stack)
    {
        return false;
    }

    private void moveTail()
    {
        this.tailCounter = 1;
    }

    /**
     * Called when the mob's health reaches 0.
     */
    public void onDeath(DamageSource cause)
    {
        super.onDeath(cause);

        if (!this.worldObj.isRemote)
        {
            this.dropChestItems();
        }
    }

    /**
     * Called frequently so the entity can update its state every tick as required. For example, zombies and skeletons
     * use this to react to sunlight and start to burn.
     */
    public void onLivingUpdate()
    {
        if (this.rand.nextInt(200) == 0)
        {
            this.moveTail();
        }

        super.onLivingUpdate();

        if (!this.worldObj.isRemote)
        {
            if (this.rand.nextInt(900) == 0 && this.deathTime == 0)
            {
                this.heal(1.0F);
            }

            if (!this.isEatingHaystack() && !this.isBeingRidden() && this.rand.nextInt(300) == 0 && this.worldObj.getBlockState(new BlockPos(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY) - 1, MathHelper.floor_double(this.posZ))).getBlock() == Blocks.GRASS)
            {
                this.setEatingHaystack(true);
            }

            if (this.isEatingHaystack() && ++this.eatingHaystackCounter > 50)
            {
                this.eatingHaystackCounter = 0;
                this.setEatingHaystack(false);
            }

            if (this.isBreeding() && !this.isAdultSmilodon() && !this.isEatingHaystack())
            {
                EntitySmilodonPopulator entitysmilodon = this.getClosestSmilodon(this, 16.0D);

                if (entitysmilodon != null && this.getDistanceSqToEntity(entitysmilodon) > 4.0D)
                {
                    this.navigator.getPathToEntityLiving(entitysmilodon);
                }
            }

            /*if (this.isSkeletonTrap() && this.skeletonTrapTime++ >= 18000)
            {
                this.setDead();
            }
            */
        }
    }

    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate()
    {
        super.onUpdate();

        if (this.worldObj.isRemote && this.dataManager.isDirty())
        {
            this.dataManager.setClean();
            this.resetTexturePrefix();
        }

        if (this.openMouthCounter > 0 && ++this.openMouthCounter > 30)
        {
            this.openMouthCounter = 0;
            this.setSmilodonWatchableBoolean(128, false);
        }

        if (this.canPassengerSteer() && this.jumpRearingCounter > 0 && ++this.jumpRearingCounter > 20)
        {
            this.jumpRearingCounter = 0;
            this.setRearing(false);
        }

        if (this.tailCounter > 0 && ++this.tailCounter > 8)
        {
            this.tailCounter = 0;
        }

        if (this.sprintCounter > 0)
        {
            ++this.sprintCounter;

            if (this.sprintCounter > 300)
            {
                this.sprintCounter = 0;
            }
        }

        this.prevHeadLean = this.headLean;

        if (this.isEatingHaystack())
        {
            this.headLean += (1.0F - this.headLean) * 0.4F + 0.05F;

            if (this.headLean > 1.0F)
            {
                this.headLean = 1.0F;
            }
        }
        else
        {
            this.headLean += (0.0F - this.headLean) * 0.4F - 0.05F;

            if (this.headLean < 0.0F)
            {
                this.headLean = 0.0F;
            }
        }

        this.prevRearingAmount = this.rearingAmount;

        if (this.isRearing())
        {
            this.headLean = 0.0F;
            this.prevHeadLean = this.headLean;
            this.rearingAmount += (1.0F - this.rearingAmount) * 0.4F + 0.05F;

            if (this.rearingAmount > 1.0F)
            {
                this.rearingAmount = 1.0F;
            }
        }
        else
        {
            this.allowStandSliding = false;
            this.rearingAmount += (0.8F * this.rearingAmount * this.rearingAmount * this.rearingAmount - this.rearingAmount) * 0.6F - 0.05F;

            if (this.rearingAmount < 0.0F)
            {
                this.rearingAmount = 0.0F;
            }
        }

        this.prevMouthOpenness = this.mouthOpenness;

        if (this.getSmilodonWatchableBoolean(128))
        {
            this.mouthOpenness += (1.0F - this.mouthOpenness) * 0.7F + 0.05F;

            if (this.mouthOpenness > 1.0F)
            {
                this.mouthOpenness = 1.0F;
            }
        }
        else
        {
            this.mouthOpenness += (0.0F - this.mouthOpenness) * 0.7F - 0.05F;

            if (this.mouthOpenness < 0.0F)
            {
                this.mouthOpenness = 0.0F;
            }
        }
    }

    private void openSmilodonMouth()
    {
        if (!this.worldObj.isRemote)
        {
            this.openMouthCounter = 1;
            this.setSmilodonWatchableBoolean(128, true);
        }
    }

    /**
     * Return true if the smilodon entity ready to mate. (no rider, not riding, tame, adult, not steril...)
     */
    private boolean canMate()
    {
        return !this.isBeingRidden() && !this.isRiding() && this.isTame() && this.isAdultSmilodon() && this.canMate() && this.getHealth() >= this.getMaxHealth() && this.isInLove();}

    public void setEatingHaystack(boolean p_110227_1_)
    {
        this.setSmilodonWatchableBoolean(32, p_110227_1_);
    }

    public void setRearing(boolean rearing)
    {
        if (rearing)
        {
            this.setEatingHaystack(false);
        }

        this.setSmilodonWatchableBoolean(64, rearing);
    }

    private void makeSmilodonRear()
    {
        if (this.canPassengerSteer())
        {
            this.jumpRearingCounter = 1;
            this.setRearing(true);
        }
    }

    public void makeSmilodonRearWithSound()
    {
        this.makeSmilodonRear();
        SoundEvent soundevent = this.getAngrySound();

        if (soundevent != null)
        {
            this.playSound(soundevent, this.getSoundVolume(), this.getSoundPitch());
        }
    }

    public void dropChestItems()
    {
        this.dropItemsInChest(this, this.smilodonChest);
        this.dropChests();
    }

    private void dropItemsInChest(Entity entityIn, AnimalChest animalChestIn)
    {
        if (animalChestIn != null && !this.worldObj.isRemote)
        {
            for (int i = 0; i < animalChestIn.getSizeInventory(); ++i)
            {
                ItemStack itemstack = animalChestIn.getStackInSlot(i);

                if (itemstack != null)
                {
                    this.entityDropItem(itemstack, 0.0F);
                }
            }
        }
    }

    public boolean setTamedBy(EntityPlayer player)
    {
        this.setOwnerUniqueId(player.getUniqueID());
        this.setSmilodonTamed(true);
        return true;
    }

    /**
     * Moves the entity based on the specified heading.
     */
    public void moveEntityWithHeading(float strafe, float forward)
    {
        if (this.isBeingRidden() && this.canBeSteered() && this.isSmilodonSaddled())
        {
            EntityLivingBase entitylivingbase = (EntityLivingBase)this.getControllingPassenger();
            this.rotationYaw = entitylivingbase.rotationYaw;
            this.prevRotationYaw = this.rotationYaw;
            this.rotationPitch = entitylivingbase.rotationPitch * 0.5F;
            this.setRotation(this.rotationYaw, this.rotationPitch);
            this.renderYawOffset = this.rotationYaw;
            this.rotationYawHead = this.renderYawOffset;
            strafe = entitylivingbase.moveStrafing * 0.5F;
            forward = entitylivingbase.moveForward;

            if (forward <= 0.0F)
            {
                forward *= 0.25F;
                this.gallopTime = 0;
            }

            if (this.onGround && this.jumpPower == 0.0F && this.isRearing() && !this.allowStandSliding)
            {
                strafe = 0.0F;
                forward = 0.0F;
            }

            if (this.jumpPower > 0.0F && !this.isSmilodonJumping() && this.onGround)
            {
                this.motionY = this.getSmilodonJumpStrength() * (double)this.jumpPower;

                if (this.isPotionActive(MobEffects.JUMP_BOOST))
                {
                    this.motionY += (double)((float)(this.getActivePotionEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1F);
                }

                this.setSmilodonJumping(true);
                this.isAirBorne = true;

                if (forward > 0.0F)
                {
                    float f = MathHelper.sin(this.rotationYaw * 0.017453292F);
                    float f1 = MathHelper.cos(this.rotationYaw * 0.017453292F);
                    this.motionX += (double)(-0.4F * f * this.jumpPower);
                    this.motionZ += (double)(0.4F * f1 * this.jumpPower);
                    this.playSound(SoundEvents.ENTITY_HORSE_JUMP, 0.4F, 1.0F);
                }

                this.jumpPower = 0.0F;
                net.minecraftforge.common.ForgeHooks.onLivingJump(this);
            }

            this.jumpMovementFactor = this.getAIMoveSpeed() * 0.1F;

            if (this.canPassengerSteer())
            {
                this.setAIMoveSpeed((float)this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
                super.moveEntityWithHeading(strafe, forward);
            }
            else if (entitylivingbase instanceof EntityPlayer)
            {
                this.motionX = 0.0D;
                this.motionY = 0.0D;
                this.motionZ = 0.0D;
            }

            if (this.onGround)
            {
                this.jumpPower = 0.0F;
                this.setSmilodonJumping(false);
            }

            this.prevLimbSwingAmount = this.limbSwingAmount;
            double d1 = this.posX - this.prevPosX;
            double d0 = this.posZ - this.prevPosZ;
            float f2 = MathHelper.sqrt_double(d1 * d1 + d0 * d0) * 4.0F;

            if (f2 > 1.0F)
            {
                f2 = 1.0F;
            }

            this.limbSwingAmount += (f2 - this.limbSwingAmount) * 0.4F;
            this.limbSwing += this.limbSwingAmount;
        }
        else
        {
            this.jumpMovementFactor = 0.02F;
            super.moveEntityWithHeading(strafe, forward);
        }
    }

    public static void registerFixesSmilodon(DataFixer fixer)
    {
        EntityLiving.registerFixesMob(fixer, "EntitySmilodonPopulator");
        fixer.registerWalker(FixTypes.ENTITY, new ItemStackDataLists("EntitySmilodonPopulator", new String[] {"Items"}));
        fixer.registerWalker(FixTypes.ENTITY, new ItemStackData("EntitySmilodonPopulator", new String[] {"ArmorItem", "SaddleItem"}));
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    public void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        compound.setBoolean("EatingHaystack", this.isEatingHaystack());
        compound.setBoolean("ChestedSmilodon", this.isChested());
        compound.setBoolean("HasReproduced", this.getHasReproduced());
        compound.setBoolean("Bred", this.isBreeding());
        //compound.setInteger("Type", this.getType().getOrdinal());
        //compound.setInteger("Variant", this.getSmilodonVariant());
        compound.setInteger("Temper", this.getTemper());
        compound.setBoolean("Tame", this.isTame());
        compound.setInteger("Mode", this.mode);
        //compound.setBoolean("SkeletonTrap", this.isSkeletonTrap());
        //compound.setInteger("SkeletonTrapTime", this.skeletonTrapTime);

        if (this.getOwnerUniqueId() != null)
        {
            compound.setString("OwnerUUID", this.getOwnerUniqueId().toString());
        }

        if (this.isChested())
        {
            NBTTagList nbttaglist = new NBTTagList();

            for (int i = 2; i < this.smilodonChest.getSizeInventory(); ++i)
            {
                ItemStack itemstack = this.smilodonChest.getStackInSlot(i);

                if (itemstack != null)
                {
                    NBTTagCompound nbttagcompound = new NBTTagCompound();
                    nbttagcompound.setByte("Slot", (byte)i);
                    itemstack.writeToNBT(nbttagcompound);
                    nbttaglist.appendTag(nbttagcompound);
                }
            }

            compound.setTag("Items", nbttaglist);
        }

        if (this.smilodonChest.getStackInSlot(1) != null)
        {
            compound.setTag("ArmorItem", this.smilodonChest.getStackInSlot(1).writeToNBT(new NBTTagCompound()));
        }

        if (this.smilodonChest.getStackInSlot(0) != null)
        {
            compound.setTag("SaddleItem", this.smilodonChest.getStackInSlot(0).writeToNBT(new NBTTagCompound()));
        }
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);
        this.setEatingHaystack(compound.getBoolean("EatingHaystack"));
        this.setBreeding(compound.getBoolean("Bred"));
        this.setChested(compound.getBoolean("ChestedSmilodon"));
        this.setHasReproduced(compound.getBoolean("HasReproduced"));
        //this.setType(SmilodonType.getArmorType(compound.getInteger("Type")));
        //this.setSmilodonVariant(compound.getInteger("Variant"));
        this.setmode(compound.getInteger("Mode"));
        this.setTemper(compound.getInteger("Temper"));
        this.setSmilodonTamed(compound.getBoolean("Tame"));
        //this.setSkeletonTrap(compound.getBoolean("SkeletonTrap"));
        //this.skeletonTrapTime = compound.getInteger("SkeletonTrapTime");
        String s;

        if (compound.hasKey("OwnerUUID", 8))
        {
            s = compound.getString("OwnerUUID");
        }
        else
        {
            String s1 = compound.getString("Owner");
            s = PreYggdrasilConverter.convertMobOwnerIfNeeded(this.getServer(), s1);
        }

        if (!s.isEmpty())
        {
            this.setOwnerUniqueId(UUID.fromString(s));
        }

        IAttributeInstance iattributeinstance = this.getAttributeMap().getAttributeInstanceByName("Speed");

        if (iattributeinstance != null)
        {
            this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(iattributeinstance.getBaseValue() * 0.25D);
        }

        if (this.isChested())
        {
            NBTTagList nbttaglist = compound.getTagList("Items", 10);
            this.initSmilodonChest();

            for (int i = 0; i < nbttaglist.tagCount(); ++i)
            {
                NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
                int j = nbttagcompound.getByte("Slot") & 255;

                if (j >= 2 && j < this.smilodonChest.getSizeInventory())
                {
                    this.smilodonChest.setInventorySlotContents(j, ItemStack.loadItemStackFromNBT(nbttagcompound));
                }
            }
        }
/*
        if (compound.hasKey("ArmorItem", 10))
        {
            ItemStack itemstack = ItemStack.loadItemStackFromNBT(compound.getCompoundTag("ArmorItem"));

            if (itemstack != null && SmilodonArmorType.isSmilodonArmor(itemstack.getItem()))
            {
                this.smilodonChest.setInventorySlotContents(1, itemstack);
            }
        }
*/
        if (compound.hasKey("SaddleItem", 10))
        {
            ItemStack itemstack1 = ItemStack.loadItemStackFromNBT(compound.getCompoundTag("SaddleItem"));

            if (itemstack1 != null && itemstack1.getItem() == Items.SADDLE)
            {
                this.smilodonChest.setInventorySlotContents(0, itemstack1);
            }
        }

        this.updateSmilodonSlots();
    }

    /**
     * Returns true if the mob is currently able to mate with the specified mob.
     */
    public boolean canMateWith(EntityAnimal otherAnimal)
    {
        if (otherAnimal == this)
        {
            return false;
        }
        else if (otherAnimal.getClass() != this.getClass())
        {
            return false;
        }
        else
        {
            EntitySmilodonPopulator entitysmilodon = (EntitySmilodonPopulator)otherAnimal;

            if (this.canMate() && entitysmilodon.canMate())
            {
                return this == entitysmilodon;
            }
            else
            {
                return false;
            }
        }
    }

    public EntityAgeable createChild(EntityAgeable ageable)
    {
        /*EntitySmilodonPopulator entitysmilodon = (EntitySmilodonPopulator)ageable;
        EntitySmilodonPopulator entitysmilodon1 = new EntitySmilodonPopulator(this.worldObj);
        SmilodonType smilodontype = this.getType();
        SmilodonType smilodontype1 = entitysmilodon.getType();
        SmilodonType smilodontype2 = SmilodonType.HORSE;

        if (smilodontype == smilodontype1)
        {
            smilodontype2 = smilodontype;
        }
        else if (smilodontype == SmilodonType.HORSE && smilodontype1 == SmilodonType.DONKEY || smilodontype == SmilodonType.DONKEY && smilodontype1 == SmilodonType.HORSE)
        {
            smilodontype2 = SmilodonType.MULE;
        }

        if (smilodontype2 == SmilodonType.HORSE)
        {
            int j = this.rand.nextInt(9);
            int i;

            if (j < 4)
            {
                i = this.getSmilodonVariant() & 255;
            }
            else if (j < 8)
            {
                i = entitysmilodon.getSmilodonVariant() & 255;
            }
            else
            {
                i = this.rand.nextInt(7);
            }

            int k = this.rand.nextInt(5);

            if (k < 2)
            {
                i = i | this.getSmilodonVariant() & 65280;
            }
            else if (k < 4)
            {
                i = i | entitysmilodon.getSmilodonVariant() & 65280;
            }
            else
            {
                i = i | this.rand.nextInt(5) << 8 & 65280;
            }

            entitysmilodon1.setSmilodonVariant(i);
        }

        entitysmilodon1.setType(smilodontype2);
        double d1 = this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue() + ageable.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue() + (double)this.getModifiedMaxHealth();
        entitysmilodon1.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(d1 / 3.0D);
        double d2 = this.getEntityAttribute(JUMP_STRENGTH).getBaseValue() + ageable.getEntityAttribute(JUMP_STRENGTH).getBaseValue() + this.getModifiedJumpStrength();
        entitysmilodon1.getEntityAttribute(JUMP_STRENGTH).setBaseValue(d2 / 3.0D);
        double d0 = this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue() + ageable.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue() + this.getModifiedMovementSpeed();
        entitysmilodon1.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(d0 / 3.0D);
        return entitysmilodon1;
        */
        return new EntitySmilodonPopulator(this.worldObj);
    }

    /**
     * Called only once on an entity when first time spawned, via egg, mob spawner, natural spawning etc, but not called
     * when entity is reloaded from nbt. Mainly used for initializing attributes and inventory
     */
    /*
    @Nullable
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata)
    {
        livingdata = super.onInitialSpawn(difficulty, livingdata);
        int i = 0;
        //SmilodonType smilodontype;

        if (livingdata instanceof EntitySmilodonPopulator.GroupData)
        {
            //smilodontype = ((EntitySmilodonPopulator.GroupData)livingdata).smilodonType;
            //i = ((EntitySmilodonPopulator.GroupData)livingdata).smilodonVariant & 255 | this.rand.nextInt(5) << 8;
        }
        else
        {
            if (this.rand.nextInt(10) == 0)
            {
                smilodontype = SmilodonType.DONKEY;
            }
            else
            {
                int j = this.rand.nextInt(7);
                int k = this.rand.nextInt(5);
                //smilodontype = SmilodonType.HORSE;
                i = j | k << 8;
            }

            livingdata = new EntitySmilodonPopulator.GroupData(smilodontype, i);
        }

        //this.setType(smilodontype);
        //this.setSmilodonVariant(i);

        if (this.rand.nextInt(5) == 0)
        {
            this.setGrowingAge(-24000);
        }
        else
        {
            this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue((double)this.getModifiedMaxHealth());

            if (smilodontype == SmilodonType.HORSE)
            {
                this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(this.getModifiedMovementSpeed());
            }
            else
            {
                this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.17499999701976776D);
            }
        }

        if (smilodontype.hasMuleEars())
        {
            this.getEntityAttribute(JUMP_STRENGTH).setBaseValue(0.5D);
        }
        else
        {
            this.getEntityAttribute(JUMP_STRENGTH).setBaseValue(this.getModifiedJumpStrength());
        }

        this.setHealth(this.getMaxHealth());
        return livingdata;
    }

*/

    public void setmode(int i) {
        this.mode = i;
    }
    /**
     * returns true if all the conditions for steering the entity are met. For pigs, this is true if it is being ridden
     * by a player and the player is holding a carrot-on-a-stick
     */
    public boolean canBeSteered()
    {
        Entity entity = this.getControllingPassenger();
        return entity instanceof EntityLivingBase;
    }

    @SideOnly(Side.CLIENT)
    public float getGrassEatingAmount(float p_110258_1_)
    {
        return this.prevHeadLean + (this.headLean - this.prevHeadLean) * p_110258_1_;
    }

    @SideOnly(Side.CLIENT)
    public float getRearingAmount(float p_110223_1_)
    {
        return this.prevRearingAmount + (this.rearingAmount - this.prevRearingAmount) * p_110223_1_;
    }

    @SideOnly(Side.CLIENT)
    public float getMouthOpennessAngle(float p_110201_1_)
    {
        return this.prevMouthOpenness + (this.mouthOpenness - this.prevMouthOpenness) * p_110201_1_;
    }

    @SideOnly(Side.CLIENT)
    public void setJumpPower(int jumpPowerIn)
    {
        if (this.isSmilodonSaddled())
        {
            if (jumpPowerIn < 0)
            {
                jumpPowerIn = 0;
            }
            else
            {
                this.allowStandSliding = true;
                this.makeSmilodonRear();
            }

            if (jumpPowerIn >= 90)
            {
                this.jumpPower = 1.0F;
            }
            else
            {
                this.jumpPower = 0.4F + 0.4F * (float)jumpPowerIn / 90.0F;
            }
        }
    }

    public boolean canJump()
    {
        return this.isSmilodonSaddled();
    }

    public void handleStartJump(int p_184775_1_)
    {
        this.allowStandSliding = true;
        this.makeSmilodonRear();
    }

    public void handleStopJump()
    {
    }

    /**
     * "Spawns particles for the smilodon entity. par1 tells whether to spawn hearts. If it is false, it spawns smoke."
     */
    @SideOnly(Side.CLIENT)
    protected void spawnSmilodonParticles(boolean p_110216_1_)
    {
        EnumParticleTypes enumparticletypes = p_110216_1_ ? EnumParticleTypes.HEART : EnumParticleTypes.SMOKE_NORMAL;

        for (int i = 0; i < 7; ++i)
        {
            double d0 = this.rand.nextGaussian() * 0.02D;
            double d1 = this.rand.nextGaussian() * 0.02D;
            double d2 = this.rand.nextGaussian() * 0.02D;
            this.worldObj.spawnParticle(enumparticletypes, this.posX + (double)(this.rand.nextFloat() * this.width * 2.0F) - (double)this.width, this.posY + 0.5D + (double)(this.rand.nextFloat() * this.height), this.posZ + (double)(this.rand.nextFloat() * this.width * 2.0F) - (double)this.width, d0, d1, d2, new int[0]);
        }
    }

    @SideOnly(Side.CLIENT)
    public void handleStatusUpdate(byte id)
    {
        if (id == 7)
        {
            this.spawnSmilodonParticles(true);
        }
        else if (id == 6)
        {
            this.spawnSmilodonParticles(false);
        }
        else
        {
            super.handleStatusUpdate(id);
        }
    }

    public void updatePassenger(Entity passenger)
    {
        super.updatePassenger(passenger);

        if (passenger instanceof EntityLiving)
        {
            EntityLiving entityliving = (EntityLiving)passenger;
            this.renderYawOffset = entityliving.renderYawOffset;
        }

        if (this.prevRearingAmount > 0.0F)
        {
            float f3 = MathHelper.sin(this.renderYawOffset * 0.017453292F);
            float f = MathHelper.cos(this.renderYawOffset * 0.017453292F);
            float f1 = 0.7F * this.prevRearingAmount;
            float f2 = 0.15F * this.prevRearingAmount;
            passenger.setPosition(this.posX + (double)(f1 * f3), this.posY + this.getMountedYOffset() + passenger.getYOffset() + (double)f2, this.posZ - (double)(f1 * f));

            if (passenger instanceof EntityLivingBase)
            {
                ((EntityLivingBase)passenger).renderYawOffset = this.renderYawOffset;
            }
        }
    }

    /**
     * Returns the Y offset from the entity's position for any entity riding this one.
     */
    public double getMountedYOffset()
    {
        double d0 = super.getMountedYOffset();

        /*
        if (this.getType() == SmilodonType.SKELETON)

        {
            d0 -= 0.1875D;
        }
        else if (this.getType() == SmilodonType.DONKEY)
        {
            d0 -= 0.25D;
        }
        */

        return d0;
    }

    /**
     * Returns randomized max health
     */
    private float getModifiedMaxHealth()
    {
        return 15.0F + (float)this.rand.nextInt(8) + (float)this.rand.nextInt(9);
    }

    /**
     * Returns randomized jump strength
     */
    private double getModifiedJumpStrength()
    {
        return 0.4000000059604645D + this.rand.nextDouble() * 0.2D + this.rand.nextDouble() * 0.2D + this.rand.nextDouble() * 0.2D;
    }

    /**
     * Returns randomized movement speed
     */
    private double getModifiedMovementSpeed()
    {
        return (0.44999998807907104D + this.rand.nextDouble() * 0.3D + this.rand.nextDouble() * 0.3D + this.rand.nextDouble() * 0.3D) * 0.25D;
    }
/*
    public boolean isSkeletonTrap()
    {
        return this.skeletonTrap;
    }

    public void setSkeletonTrap(boolean skeletonTrapIn)
    {
        if (skeletonTrapIn != this.skeletonTrap)
        {
            this.skeletonTrap = skeletonTrapIn;

            if (skeletonTrapIn)
            {
                this.tasks.addTask(1, this.skeletonTrapAI);
            }
            else
            {
                this.tasks.removeTask(this.skeletonTrapAI);
            }
        }
    }
*/
    /**
     * returns true if this entity is by a ladder, false otherwise
     */
    public boolean isOnLadder()
    {
        return false;
    }

    public float getEyeHeight()
    {
        return this.height;
    }

    public boolean replaceItemInInventory(int inventorySlot, @Nullable ItemStack itemStackIn)
    {
        if (inventorySlot == 499)
        {
            if (itemStackIn == null && this.isChested())
            {
                this.setChested(false);
                this.initSmilodonChest();
                return true;
            }

            if (itemStackIn != null && itemStackIn.getItem() == Item.getItemFromBlock(Blocks.CHEST) && !this.isChested())
            {
                this.setChested(true);
                this.initSmilodonChest();
                return true;
            }
        }

        int i = inventorySlot - 400;

        if (i >= 0 && i < 2 && i < this.smilodonChest.getSizeInventory())
        {
            if (i == 0 && itemStackIn != null && itemStackIn.getItem() != Items.SADDLE)
            {
                return false;
            }
         /*   else if (i != 1 || (itemStackIn == null || SmilodonArmorType.isSmilodonArmor(itemStackIn.getItem())) && this.getType().isSmilodon())
            {
                this.smilodonChest.setInventorySlotContents(i, itemStackIn);
                this.updateSmilodonSlots();
                return true;
            }
            */
            else
            {
                return false;
            }
        }
        else
        {
            int j = inventorySlot - 500 + 2;

            if (j >= 2 && j < this.smilodonChest.getSizeInventory())
            {
                this.smilodonChest.setInventorySlotContents(j, itemStackIn);
                return true;
            }
            else
            {
                return false;
            }
        }
    }

    /**
     * For vehicles, the first passenger is generally considered the controller and "drives" the vehicle. For example,
     * Pigs, Smilodons, and Boats are generally "steered" by the controlling passenger.
     */
    @Nullable
    public Entity getControllingPassenger()
    {
        return this.getPassengers().isEmpty() ? null : (Entity)this.getPassengers().get(0);
    }

    /**
     * Get this Entity's EnumCreatureAttribute
     */
    public EnumCreatureAttribute getCreatureAttribute()
    {
        return EnumCreatureAttribute.UNDEFINED;
    }

    /*
    @Nullable
    protected ResourceLocation getLootTable()
    {
        return this.lootTable;
    }
    */

    /*public static class GroupData implements IEntityLivingData
    {
        //public SmilodonType smilodonType;
        public int smilodonVariant;

        public GroupData(SmilodonType p_i46589_1_, int p_i46589_2_)
        {
            this.smilodonType = p_i46589_1_;
            this.smilodonVariant = p_i46589_2_;
        }
    }
*/
    // FORGE
    private net.minecraftforge.items.IItemHandler itemHandler = null; // Initialized by initSmilodonChest above.

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, net.minecraft.util.EnumFacing facing)
    {
        if (capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return (T) itemHandler;
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability, net.minecraft.util.EnumFacing facing)
    {
        return capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }
}