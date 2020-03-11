<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateHwidTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('users_hwids', function (Blueprint $table) {
            $table->bigIncrements('id');
            $table->tinyInteger('isBanned')->default('0');
            $table->text('totalMemory');
            $table->text('serialNumber');
            $table->text('HWDiskSerial');
            $table->text('processorID');
            $table->text('macAddr');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('users_hwids');
    }
}
